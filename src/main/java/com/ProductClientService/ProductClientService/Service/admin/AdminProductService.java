package com.ProductClientService.ProductClientService.Service.admin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.common.protocol.types.Field.Str;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.admin.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.admin.CategoryAttributeRequest;
import com.ProductClientService.ProductClientService.DTO.admin.CategoryDto;
import com.ProductClientService.ProductClientService.DTO.seller.CategoryAttributeDto;
import com.ProductClientService.ProductClientService.Model.Attribute;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.CategoryAttribute;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Repository.AttributeRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminProductService {
    private final HttpServletRequest request;
    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final JdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    public ApiResponse<Object> addCategory(CategoryDto categoryRequest) {
        // boolean isValid = addToCategory(categoryRequest);
        // if (!isValid)
        // return new ApiResponse<>(false, "Failed To Add", null, 500);
        return new ApiResponse<>(true, "Category Added", null, 201);
    }

    @Async
    public void addCategoryFromJsonFile() throws IOException {
        System.out.println("Step 1: Thread Start");

        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("category.json");

        if (inputStream == null) {
            throw new FileNotFoundException("Step 2: category.json not found in resources!");
        }
        System.out.println("Step 2: category.json file loaded successfully");

        JsonNode rootNode = mapper.readTree(inputStream);
        System.out.println("Step 3: JSON file parsed into JsonNode");

        JsonNode itemsNode = rootNode.get("items");
        System.out.println("Step 4: Extracted 'items' node from JSON");

        if (itemsNode == null) {
            System.out.println("Step 4.1: 'items' node is NULL");
            return;
        }

        if (!itemsNode.isArray()) {
            System.out.println("Step 4.2: 'items' node is NOT an array");
        } else {
            System.out.println("Step 4.3: 'items' node is an array with size = " + itemsNode.size());
        }

        // Loop for super-category
        for (JsonNode item : itemsNode) {
            if (true)
                break;
            String type = item.get("type").asText();
            System.out.println("Step 5: Checking item type = " + type);

            JsonNode dataArray = item.get("data");
            if (dataArray == null) {
                System.out.println("Step 5.1: data array is NULL for type = " + type);
                continue;
            }

            if ("super-category".equals(type)) { // FIX: use equals instead of ==
                System.out.println("Step 6: Found super-category with size = " + dataArray.size());
                for (JsonNode supercategorydata : dataArray) {
                    String name = supercategorydata.get("name").asText();
                    Integer id = supercategorydata.get("id").asInt();
                    Integer parent_id = supercategorydata.get("parent_id") == null ? 0
                            : supercategorydata.get("parent_id").asInt();
                    System.out.println("Step 6.1: Super-category name = " + name);

                    System.out.println("Step 6.2: Call The DB To Insert The Category");
                    addToCategory(new CategoryDto(name, null), Category.Level.SUPER_CATEGORY, id, parent_id);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Step 6.3: Thread interrupted while inserting super-category");
                    }
                }
            }
        }

        // Loop for category
        for (JsonNode item : itemsNode) {
            if (true)
                break;
            String type = item.get("type").asText();
            JsonNode dataArray = item.get("data");

            if ("category".equals(type)) {
                System.out.println("Step 7: Found category with size = " + dataArray.size());
                for (JsonNode categoryData : dataArray) {
                    String childname = categoryData.get("name").asText();
                    String parent_name = categoryData.get("parent_name").asText();
                    Integer id = categoryData.get("id").asInt();
                    Integer parent_id = categoryData.get("parent_id").asInt();
                    System.out.println("Step 7.1: Category name = " + childname + ", parent = " + parent_name);

                    addToCategory(new CategoryDto(childname, parent_name), Category.Level.CATEGORY, id, parent_id);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Step 7.2: Thread interrupted while inserting category");
                    }
                }
            }
        }

        // Loop for sub-category
        for (JsonNode item : itemsNode) {
            if (true)
                break;
            String type = item.get("type").asText();
            JsonNode dataArray = item.get("data");

            if ("sub-category".equals(type)) {
                System.out.println("Step 8: Found sub-category with size = " + dataArray.size());
                for (JsonNode subcategoryData : dataArray) {
                    String childname = subcategoryData.get("name").asText();
                    String parent_name = subcategoryData.get("parent_name").asText();
                    Integer id = subcategoryData.get("id").asInt();
                    Integer parent_id = subcategoryData.get("parent_id").asInt();
                    System.out.println("Step 8.1: Sub-category name = " + childname + ", parent = " + parent_name);

                    addToCategory(new CategoryDto(childname, parent_name), Category.Level.SUBCATEGORY, id, parent_id);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Step 8.2: Thread interrupted while inserting sub-category");
                    }
                }
            }
        }

        // Loop for sub-sub-category
        for (JsonNode item : itemsNode) {
            String type = item.get("type").asText();
            JsonNode dataArray = item.get("data");
            if ("sub-sub-category".equals(type)) {
                System.out.println("Step 9: Found sub-sub-category with size = " + dataArray.size());
                boolean flag = false;
                List<Category> batch = new ArrayList<>();
                int batchSize = 25;

                for (JsonNode subsubcategoryData : dataArray) {
                    String childname = subsubcategoryData.get("name").asText();
                    String parent_name = subsubcategoryData.get("parent_name").asText();
                    Integer id = subsubcategoryData.get("id").asInt();
                    Integer parent_id = subsubcategoryData.get("parent_id").asInt();

                    if (id == 13383 && !flag) {
                        flag = true;
                        System.out.println("condition met at id 12686");
                        continue;
                    }
                    if (!flag)
                        continue;

                    // build Category (no save yet)
                    Category category = buildCategory(new CategoryDto(childname, parent_name),
                            Category.Level.SUBSUBCATEGORY,
                            id, parent_id);

                    if (category != null) {
                        batch.add(category);
                    }

                    // Insert every 25 records
                    if (batch.size() == batchSize) {
                        insertBatch(batch);
                        batch.clear();
                        System.out.println("Inserted 25 sub-sub-categories");
                    }
                }

                // Insert any leftovers
                if (!batch.isEmpty()) {
                    insertBatch(batch);
                    System.out.println("Inserted remaining " + batch.size() + " sub-sub-categories");
                }
            }
        }

        System.out.println("Step 10: Completed processing category.json");
    }

    private Category buildCategory(CategoryDto categoryRequest, Category.Level level,
            Integer externalId, Integer parentExternalId) {
        Category category = new Category();
        category.setName(categoryRequest.category());

        if (categoryRequest.parent() != null) {
            Category.Level parentLevel = (level == Category.Level.CATEGORY) ? Category.Level.SUPER_CATEGORY
                    : (level == Category.Level.SUBCATEGORY) ? Category.Level.CATEGORY
                            : (level == Category.Level.SUBSUBCATEGORY) ? Category.Level.SUBCATEGORY : null;

            Optional<Category> parentCategory = categoryRepository
                    .findFirstByExternalIdAndCategoryLevel(parentExternalId, parentLevel);

            if (parentCategory.isEmpty()) {
                System.out.println("No Value in Db " + parentExternalId + " " + level);
                return null; // skip
            }
            category.setParent(parentCategory.get());
        }

        category.setExternalId(externalId);
        category.setCategoryLevel(level);
        category.setMax_products(9);
        category.setMin_products(1);
        return category;
    }

    public void insertBatch(List<Category> categories) {

        String sql = "INSERT INTO categories (id, name, external_id, category_level, parent_id, max_products, min_products) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, categories, 25, (ps, category) -> {
            UUID id = UUID.randomUUID();
            ps.setObject(1, id, java.sql.Types.OTHER); // for UUID in PostgreSQL
            ps.setString(2, category.getName());
            ps.setInt(3, category.getExternalId());
            ps.setInt(4, category.getCategoryLevel().ordinal());
            if (category.getParent() != null) {
                ps.setObject(5, category.getParent().getId(), java.sql.Types.OTHER);
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            ps.setInt(6, category.getMax_products());
            ps.setInt(7, category.getMin_products());
        });
    }

    @Transactional
    public ApiResponse<Object> addCategoryAttribute(CategoryAttributeRequest request) {
        Category categoryRef = entityManager.getReference(Category.class, request.categoryId());
        Attribute attributeRef = entityManager.getReference(Attribute.class, request.attributeId());

        CategoryAttribute categoryAttribute = categoryAttributeRepository
                .findByCategoryId(request.categoryId())
                .orElseGet(() -> {
                    CategoryAttribute newCategoryAttribute = new CategoryAttribute();
                    newCategoryAttribute.setCategory(categoryRef);
                    return newCategoryAttribute;
                });

        // Directly add to the Set (no need to check manually, Set avoids duplicates)
        categoryAttribute.getAttributes().add(attributeRef);

        categoryAttribute = categoryAttributeRepository.save(categoryAttribute);

        CategoryAttributeDto dto = new CategoryAttributeDto(
                categoryAttribute.getId(),
                categoryAttribute.getCategory().getId(),
                categoryAttribute.getAttributes()
                        .stream()
                        .map(attr -> new AttributeDto(
                                attr.getName(),
                                attr.getField_type(),
                                attr.getIs_required(),
                                attr.getOptions()))
                        .toList());

        return new ApiResponse<>(true, "Added Successfully", dto, 201);
    }

    public ApiResponse<Object> addAttribute(AttributeDto categoryRequest) {
        System.out.println("we are here bro 1");
        boolean isValid = createAttribute(categoryRequest);
        System.out.println("we are here bro 3");
        if (!isValid)
            return new ApiResponse<>(false, "Failed To Add", null, 500);
        return new ApiResponse<>(true, "Attribute Added", null, 201);
    }

    public ApiResponse<Object> updateAttributefun(UUID id, AttributeDto attributeRequest) {
        boolean isValid = updateAttribute(id, attributeRequest);
        if (!isValid)
            return new ApiResponse<>(false, "Failed To Update", null, 500);
        return new ApiResponse<>(true, "Attribute Updated", null, 201);
    }
    // public ApiResponse<Object> searchCategory(String name){

    // }
    public boolean addToCategory(CategoryDto categoryRequest, Category.Level level, Integer externalId,
            Integer parentExternalId) {
        Category category = new Category();
        category.setName(categoryRequest.category());

        // If parent name is given
        if (categoryRequest.parent() != null) {
            Category.Level passvalue;
            if (Category.Level.CATEGORY == level) {
                passvalue = Category.Level.SUPER_CATEGORY;
            } else if (Category.Level.SUBCATEGORY == level) {
                passvalue = Category.Level.CATEGORY;
            } else if (Category.Level.SUBSUBCATEGORY == level) {
                passvalue = Category.Level.SUBCATEGORY;
            } else {
                passvalue = null;
            }
            Optional<Category> parentCategory = categoryRepository
                    .findFirstByExternalIdAndCategoryLevel(parentExternalId, passvalue);

            if (parentCategory.isEmpty()) {
                System.out.println("No Value in Db" + parentExternalId + ' ' + level);
                return false;
            }
            category.setParent(parentCategory.get());
        }
        category.setExternalId(externalId);
        category.setCategoryLevel(level);
        category.setMax_products(9);
        category.setMin_products(1);
        Category response = categoryRepository.save(category);
        if (response == null) {
            System.out.println("Not Able To Save The Category");
        }
        return true;
    }

    public Boolean createAttribute(AttributeDto dto) {
        // check if attribute already exists
        System.out.println("we are here 1");
        if (attributeRepository.existsByName(dto.name())) {
            System.out.println("we are firing query");
            throw new RuntimeException("Attribute with name '" + dto.name() + "' already exists!");
        }
        System.out.println("we are here 2");
        Attribute attribute = new Attribute();
        System.out.println("we are here 3");
        attribute.setName(dto.name());
        System.out.println("we are here 4");
        attribute.setField_type(dto.fieldType());
        attribute.setIs_required(dto.isRequired() != null ? dto.isRequired() : true);
        attribute.setOptions(dto.options());

        return attributeRepository.save(attribute) != null;
    }

    public Boolean updateAttribute(UUID id, AttributeDto dto) {
        try {
            Attribute attribute = attributeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Attribute not found with id " + id));
            if (dto.name() != null && !dto.name().isEmpty())
                attribute.setName(dto.name());
            if (dto.fieldType() != null)
                attribute.setField_type(dto.fieldType());
            if (dto.isRequired() != null)
                attribute.setIs_required(dto.isRequired());
            if (dto.options() != null && !dto.options().isEmpty()) {
                List<String> existingOptions = new ArrayList<>();
                if (attribute.getOptions() != null) {
                    existingOptions = attribute.getOptions();
                }
                List<String> newOptions = dto.options();
                for (String opt : newOptions) {
                    if (!existingOptions.contains(opt)) {
                        existingOptions.add(opt);
                    }
                }
                attribute.setOptions(existingOptions);
            }
            return attributeRepository.save(attribute) != null;
        } catch (Exception e) {
            throw new RuntimeException("Error updating options JSON", e);
        }

    }

    public CategoryAttribute createCategoryAttribute(UUID categoryId, UUID attributeId,
            Boolean isRequired, Boolean isImageAttribute, Boolean isVariantAttribute) {
        try {
            // fetch category
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

            // fetch attribute
            Attribute attribute = attributeRepository.findById(attributeId)
                    .orElseThrow(() -> new EntityNotFoundException("Attribute not found with id: " + attributeId));

            // create and link
            CategoryAttribute categoryAttribute = new CategoryAttribute();
            categoryAttribute.setCategory(category);
            categoryAttribute.setAttributes(new HashSet<>() {
                {
                    add(attribute);
                }
            });
            categoryAttribute.setIs_Required(isRequired != null ? isRequired : false);
            categoryAttribute.setIsImageAttribute(isImageAttribute != null ? isImageAttribute : false);
            categoryAttribute.setIsVariantAttribute(isVariantAttribute != null ? isVariantAttribute : false);

            return categoryAttributeRepository.save(categoryAttribute);
        } catch (Exception e) {
            throw new RuntimeException("Error creating category attribute: " + e.getMessage(), e);
        }
    }
}
// hkiyfhgyui hiuydi hggdyu buhuf huiy78dhghuygujhgui hihuk igihuihhuiiu
// khuiuhhuihi huhjniy gygyu hyhui gyuybyhiuyub yuiujuhiujn hjhu gyhhu huih huhj
// hihui huhuhhuihiu ghhiuhuhuthe overnkjhiuhu bhhihui nihjuijiujiuj hijuiji
// njkhuih huihuihhihuihuiuhu iujiojioj hhyuhi huihui huihuih huhuih huiuji
// hiuy78ifkuuii uhuyi hui huiyui huiyuiy huiyuiy hujhyuiyhiuh njkiuhui hiuyhiy
// gyuyuyu ugyyuy