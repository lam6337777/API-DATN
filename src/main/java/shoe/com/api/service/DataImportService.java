package shoe.com.api.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import lombok.extern.slf4j.Slf4j;
import shoe.com.api.dto.request.ProductRequest;
import shoe.com.api.entity.BrandEntity;
import shoe.com.api.entity.CategoryEntity;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.repository.BrandRepository;
import shoe.com.api.repository.CategoryRepository;

@Service
@Slf4j
public class DataImportService {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public DataImportService(ProductService productService,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    public int importProductsFromExcel(MultipartFile file) {
        int successCount = 0;
        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Bỏ qua header
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                try {
                    ProductRequest request = mapRowToProductRequest(currentRow);
                    if (request != null) {
                        productService.createProduct(request);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi import dòng {}: {}", currentRow.getRowNum() + 1, e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new BadRequestException("Lỗi khi đọc file Excel: " + e.getMessage());
        }
        return successCount;
    }

    public int importProductsFromCsv(MultipartFile file) {
        int successCount = 0;
        int failureCount = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVReader csvReader = new CSVReader(reader)) {

            String[] nextLine;
            // Bỏ qua header
            csvReader.readNext();

            int rowNum = 1;
            while ((nextLine = csvReader.readNext()) != null) {
                rowNum++;
                try {
                    // Kiểm tra số lượng cột tối thiểu (ít nhất 10 cột theo
                    // mapStringsToProductRequest)
                    if (nextLine.length < 10) {
                        log.warn("Bỏ qua dòng {}: Không đủ số lượng cột (hiện có {})", rowNum, nextLine.length);
                        failureCount++;
                        continue;
                    }

                    ProductRequest request = mapStringsToProductRequest(nextLine);
                    if (request != null) {
                        productService.createProduct(request);
                        successCount++;
                    } else {
                        log.warn("Bỏ qua dòng {}: Dữ liệu không hợp lệ (không có tên sản phẩm)", rowNum);
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi import CSV dòng {}: {}", rowNum, e.getMessage());
                    failureCount++;
                }
            }
            log.info("Import hoàn tất: {} thành công, {} thất bại", successCount, failureCount);
        } catch (IOException | CsvValidationException e) {
            throw new BadRequestException("Lỗi khi đọc file CSV: " + e.getMessage());
        }
        return successCount;
    }

    private ProductRequest mapRowToProductRequest(Row row) {
        String[] data = new String[11];
        for (int i = 0; i < 11; i++) {
            data[i] = getCellValueAsString(row.getCell(i));
        }
        return mapStringsToProductRequest(data);
    }

    private ProductRequest mapStringsToProductRequest(String[] data) {
        // Thứ tự cột CSV dự kiến:
        // 0: product_name, 1: listing_price, 2: sale_price, 3: discount,
        // 4: brand, 5: category, 6: description, 7: rating, 8: reviews,
        // 9: images (JSON array/String list), 10: Sku (biến thể)

        String name = data[0];
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        BigDecimal listingPrice = parseToBigDecimal(data.length > 1 ? data[1] : null);
        BigDecimal salePrice = parseToBigDecimal(data.length > 2 ? data[2] : null);
        // data[3] = discount (bỏ qua)
        String brandName = data.length > 4 ? data[4] : null;
        String categoryName = data.length > 5 ? data[5] : null;
        String description = data.length > 6 ? data[6] : null;
        // data[7] = rating (bỏ qua)
        // data[8] = reviews (bỏ qua)
        String imagesStr = data.length > 9 ? data[9] : null;
        String variantsStr = data.length > 10 ? data[10] : null;

        // Xử lý giá: Nếu listing_price = 0 hoặc null, lấy sale_price làm giá gốc
        BigDecimal originalPrice = (listingPrice != null && listingPrice.compareTo(BigDecimal.ZERO) > 0)
                ? listingPrice
                : (salePrice != null ? salePrice : BigDecimal.ZERO);
        BigDecimal finalSalePrice = (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) ? salePrice : null;

        ProductRequest request = new ProductRequest();
        request.setName(name.trim());
        request.setDescription(description != null ? description.trim() : "");
        request.setOriginalPrice(originalPrice);
        request.setSalePrice(finalSalePrice);
        request.setIsFeatured(false);

        // Ánh xạ Category (tự tạo mới nếu chưa tồn tại)
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            String catName = categoryName.trim();
            CategoryEntity category = categoryRepository.findByName(catName)
                    .orElseGet(() -> {
                        CategoryEntity newCat = CategoryEntity.builder()
                                .name(catName)
                                .slug(generateSlug(catName))
                                .isActive(true)
                                .build();
                        return categoryRepository.save(newCat);
                    });
            request.setCategoryId(category.getId());
        }

        // Ánh xạ Brand (tự tạo mới nếu chưa tồn tại)
        if (brandName != null && !brandName.trim().isEmpty()) {
            String brName = brandName.trim();
            BrandEntity brand = brandRepository.findByName(brName)
                    .orElseGet(() -> {
                        BrandEntity newBrand = BrandEntity.builder()
                                .name(brName)
                                .slug(generateSlug(brName))
                                .isActive(true)
                                .build();
                        return brandRepository.save(newBrand);
                    });
            request.setBrandId(brand.getId());
        }

        // Phân tích Biến thể (format: Size:Màu:Stock:SKU|...)
        if (variantsStr != null && !variantsStr.trim().isEmpty()) {
            List<ProductRequest.VariantRequest> variants = new ArrayList<>();
            String[] parts = variantsStr.split("\\|");
            for (String part : parts) {
                if (part.trim().isEmpty())
                    continue;
                String[] vData = part.split(":");
                if (vData.length >= 2) {
                    ProductRequest.VariantRequest v = new ProductRequest.VariantRequest();
                    v.setSize(vData[0].trim());
                    if (vData.length >= 3) {
                        try {
                            v.setStockQuantity(Integer.parseInt(vData[2].trim()));
                        } catch (NumberFormatException e) {
                            v.setStockQuantity(0);
                        }
                    }
                    if (vData.length >= 4) {
                        v.setSku(vData[3].trim());
                    }
                    variants.add(v);
                }
            }
            request.setVariants(variants);
        }

        // Phân tích Ảnh (chấp nhận cả JSON array ["url1",...] hoặc danh sách phân cách
        // bởi dấu phẩy)
        if (imagesStr != null && !imagesStr.trim().isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            // Xử lý cả định dạng JSON array và text thuần
            String cleaned = imagesStr.replaceAll("[\\[\\]\"]", "");
            String[] urls = cleaned.split(",");
            for (String url : urls) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    imageUrls.add(trimmed);
                }
            }
            request.setImageUrls(imageUrls);
        }

        return request;
    }

    private BigDecimal parseToBigDecimal(String val) {
        if (val == null || val.trim().isEmpty())
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String generateSlug(String name) {
        if (name == null)
            return UUID.randomUUID().toString().substring(0, 8);
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d").replaceAll("Đ", "D")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        if (slug.isEmpty())
            slug = UUID.randomUUID().toString().substring(0, 8);
        return slug;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null)
            return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue());
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }
}
