package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shoe.com.api.entity.BrandEntity;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.BrandRepository;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<BrandEntity> getAllActiveBrands() {
        return brandRepository.findByIsActiveTrue();
    }

    public List<BrandEntity> getAllBrands() {
        return brandRepository.findAll();
    }

    public BrandEntity createBrand(String name, String description, String logoUrl) {
        if (brandRepository.existsByName(name)) {
            throw new BadRequestException("Thương hiệu '" + name + "' đã tồn tại");
        }

        String slug = generateSlug(name);
        BrandEntity brand = BrandEntity.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .logoUrl(logoUrl)
                .build();

        return brandRepository.save(brand);
    }

    public BrandEntity updateBrand(Long id, String name, String description, String logoUrl) {
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));

        brand.setName(name);
        brand.setDescription(description);
        if (logoUrl != null)
            brand.setLogoUrl(logoUrl);

        return brandRepository.save(brand);
    }

    public void deleteBrand(Long id) {
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));
        brand.setIsActive(false);
        brandRepository.save(brand);
    }

    private String generateSlug(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d").replaceAll("Đ", "D")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}
