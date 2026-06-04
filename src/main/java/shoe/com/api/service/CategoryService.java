package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shoe.com.api.entity.CategoryEntity;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.CategoryRepository;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryEntity> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    public List<CategoryEntity> getAllCategories() {
        return categoryRepository.findAll();
    }

    public CategoryEntity getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "slug", slug));
    }

    public CategoryEntity createCategory(String name, String description, String imageUrl) {
        if (categoryRepository.existsByName(name)) {
            throw new BadRequestException("Danh mục '" + name + "' đã tồn tại");
        }

        String slug = generateSlug(name);
        CategoryEntity category = CategoryEntity.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .imageUrl(imageUrl)
                .build();

        return categoryRepository.save(category);
    }

    public CategoryEntity updateCategory(String id, String name, String description, String imageUrl) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        category.setName(name);
        category.setDescription(description);
        if (imageUrl != null)
            category.setImageUrl(imageUrl);

        return categoryRepository.save(category);
    }

    public void deleteCategory(String id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));
        category.setIsActive(false);
        categoryRepository.save(category);
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
