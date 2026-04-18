package com.ProductClientService.ProductClientService.Service.seller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDocument;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.admin.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.CategoryAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductFullResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.CategoryAttribute;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductAttribute;
import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Model.Attribute;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Model.Address;
import com.ProductClientService.ProductClientService.Repository.AttributeRepository;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.ProductAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.ProductVariantRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;
import com.ProductClientService.ProductClientService.Service.OpenStreetMapService;
import com.ProductClientService.ProductClientService.Service.S3Service;
import com.ProductClientService.ProductClientService.Service.OpenStreetMapService.AddressResponse;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {
    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;
    private final ProductRepository productRepository;
    private final S3Service s3Service;
    private final CategoryRepository categoryRepository;
    private final HttpServletRequest request;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final AttributeRepository attributeRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final StandardProductRepository standardProductRepository;
    private final Cloudinary cloudinary;
    private final SellerRepository sellerRepository;
    private final BrandRepository brandRepository;
    private final OpenStreetMapService openStreetMapService;
    private final SellerAddressRepository sellerAddressRepository;
    private final EventPublisherService eventPublisher;
    @PersistenceContext
    private EntityManager entityManager;

    public ApiResponse<Object> addProduct(ProductDto dto) {

        Product product;

        // =============================
        // UPDATE FLOW
        // =============================
        if (dto.productId() != null) {

            product = productRepository.findById(dto.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

        }
        // =============================
        // CREATE FLOW
        // =============================
        else {
            product = new Product();
        }

        // Common fields
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setStep(Product.Step.valueOf(dto.step()));

        Seller seller = sellerRepository.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        product.setSeller(seller);
        if (dto.category() != null) {
            Category category = categoryRepository.findById(dto.category())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        UUID savedProductId = productRepository.save(product).getId();

        Map<String, Object> responseData = Map.of("productId", savedProductId);

        return new ApiResponse<>(
                true,
                dto.productId() == null ? "Product Created" : "Product Updated",
                responseData,
                200);
    }

    public ApiResponse<Object> getLatestDraftProduct() {
        UUID sellerId = (UUID) request.getAttribute("id");
        Optional<Product> draftProduct = productRepository
                .findTopBySellerIdAndStepNotOrderByCreatedAtDesc(
                        sellerId,
                        Product.Step.LIVE);
        if (draftProduct.isEmpty()) {
            return new ApiResponse<>(false, "No Draft Product Found", null, 200);
        }
        // ProductFullResponseDto responseDto = new ProductFullResponseDto(
        // draftProduct.get().getId(),
        // draftProduct.get().getName(),
        // draftProduct.get().getDescription(),
        // draftProduct.get().getProductAttributes().stream()
        // .map(pa -> new ProductAttributeResponseDto(
        // pa.getId(),
        // pa.getCategoryAttribute().getId(),
        // pa.getCategoryAttribute().getAttributes().stream()
        // .findFirst()
        // .map(Attribute::getName)
        // .orElse(null),
        // pa.getValue(),
        // pa.getVariants().stream()
        // .map(variant -> new ProductVariantResponseDto(
        // variant.getId(),
        // variant.getSku(),
        // variant.getPrice(),
        // variant.getStock())
        // )
        // .toList());
        return new ApiResponse<>(true, "Latest Draft Product Found", "responseDto", 200);
    }

    public ApiResponse<Object> discardDraftProduct() {

        UUID sellerId = (UUID) request.getAttribute("id");

        Optional<Product> draftProduct = productRepository
                .findTopBySellerIdAndStepNotOrderByCreatedAtDesc(
                        sellerId,
                        Product.Step.LIVE);

        if (draftProduct.isEmpty()) {
            return new ApiResponse<>(false, "No Draft Product Found", null, 200);
        }

        productRepository.delete(draftProduct.get());

        return new ApiResponse<>(true, "Draft discarded successfully", null, 200);
    }

    public ApiResponse<Object> loadAttribute(UUID id) {
        return new ApiResponse<>(true, "Step Completed", null, 200);
    }

    public ApiResponse<Object> getAttributesByCategoryId(UUID categoryId) {

        List<CategoryAttribute> categoryAttributes = categoryAttributeRepository.findAllByCategoryId(categoryId);

        if (categoryAttributes.isEmpty()) {
            return new ApiResponse<>(false,
                    "No attributes found for this category",
                    null,
                    200);
        }

        List<AttributeDto> attributeDtos = categoryAttributes.stream()
                .flatMap(ca -> ca.getAttributes().stream()
                        .map(attr -> new AttributeDto(
                                ca.getId(), // ✅ categoryAttributeId
                                attr.getName(),
                                attr.getField_type(),
                                attr.getIs_required(),
                                attr.getOptions(),
                                attr.getIsRadio(),
                                ca.getIs_Required(),
                                ca.getIsImageAttribute(),
                                ca.getIsVariantAttribute(),
                                ca.getIsAdditionalAttribute())))
                .toList();

        CategoryAttributeDto dto = new CategoryAttributeDto(
                null,
                categoryId,
                attributeDtos);

        return new ApiResponse<>(true, "fetch data", dto, 200);
    }

    public ApiResponse<Object> addProductAttribute(ProductAttributeDto dto) {
        try {
            List<ProductAttributeResponseDto> productAttributeResponseDtos = saveAllAttributes(dto);
            return new ApiResponse<>(true, "Saved In The Db", productAttributeResponseDtos, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
    public List<ProductAttributeResponseDto> saveAllAttributes(ProductAttributeDto dto) {

        if (dto.categoryAttributeId() == null || dto.values() == null) {
            throw new RuntimeException("Invalid request");
        }

        if (dto.categoryAttributeId().size() != dto.values().size()) {
            throw new RuntimeException("Attribute and value size mismatch");
        }

        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setStep(Product.Step.valueOf(dto.step()));

        // 💡 FIX: Add a flat index counter to iterate through the flat
        // productAttributeIds array
        int flatAttributeIdIndex = 0;

        for (int i = 0; i < dto.categoryAttributeId().size(); i++) {

            UUID categoryAttrId = dto.categoryAttributeId().get(i);
            List<String> vals = dto.values().get(i);

            if (vals == null || vals.isEmpty()) {
                continue;
            }

            CategoryAttribute categoryAttribute = categoryAttributeRepository.findById(categoryAttrId)
                    .orElseThrow(() -> new RuntimeException("CategoryAttribute not found"));

            for (String value : vals) {
                UUID productAttributeId = null;

                // 💡 FIX: Grab the specific ID for THIS value using the flat counter, not 'i'
                if (dto.productAttributeIds() != null
                        && dto.productAttributeIds().size() > flatAttributeIdIndex
                        && dto.productAttributeIds().get(flatAttributeIdIndex) != null) {

                    productAttributeId = dto.productAttributeIds().get(flatAttributeIdIndex);
                }

                if (productAttributeId != null) {
                    // ✅ UPDATE existing entity
                    ProductAttribute existing = productAttributeRepository.findById(productAttributeId)
                            .orElseThrow(() -> new RuntimeException("ProductAttribute not found"));

                    existing.setValue(value);
                } else {
                    // ✅ CREATE new entity
                    ProductAttribute newAttribute = new ProductAttribute();
                    newAttribute.setProduct(product);
                    newAttribute.setCategoryAttribute(categoryAttribute);
                    newAttribute.setValue(value);

                    product.getProductAttributes().add(newAttribute);
                }

                // 💡 FIX: Increment flat index after processing each individual value
                flatAttributeIdIndex++;
            }
        }

        Product savedProduct = productRepository.save(product);

        return savedProduct.getProductAttributes()
                .stream()
                .map(pa -> {
                    CategoryAttribute ca = pa.getCategoryAttribute();

                    String attributeName = ca.getAttributes()
                            .stream()
                            .findFirst()
                            .map(Attribute::getName)
                            .orElse(null);

                    return new ProductAttributeResponseDto(
                            pa.getId(),
                            ca.getId(),
                            attributeName,
                            pa.getValue(),
                            List.of());
                })
                .toList();
    }
    // public ApiResponse<Object> getProductAttributes(UUID productId) {
    // try {
    // List<ProductAttribute> attributes =
    // productAttributeRepository.findByProductIdWithAttribute(productId);

    // // Map to DTO for clean response
    // List<Map<String, Object>> response = attributes.stream().map(attr -> {
    // Map<String, Object> map = new HashMap<>();
    // map.put("attributeId", attr.getCategory_attribute().getId());
    // map.put("attributeName",
    // attr.getCategory_attribute().getCategory().getName()); // you want name tooif
    // map.put("value", attr.getValue());
    // return map;
    // }).toList();

    // return new ApiResponse<>(true, "Fetched successfully", response, 200);
    // } catch (Exception e) {
    // return new ApiResponse<>(false, e.getMessage(), null, 500);
    // }
    // }

    public ApiResponse<Object> addProductVariants(ProductVariantsDto dto) {
        try {
            Product product = productRepository.findById(dto.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStep(Product.Step.valueOf(dto.step()));

            for (int i = 0; i < dto.skus().size(); i++) {
                ProductVariant variant = new ProductVariant();
                variant.setSku(dto.skus().get(i));
                variant.setStock(Integer.parseInt(dto.stock().get(i)));
                double multipliedPrice = 100 * Double.parseDouble(dto.price().get(i));
                variant.setPrice(String.valueOf(multipliedPrice));
                variant = productVariantRepository.save(variant); // Save to get ID
                product.getVariants().add(variant);
            }
            productRepository.save(product); // Update step

            return new ApiResponse<>(true, "Variants added successfully", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> attachBrandToProduct(UUID productId, UUID brandId) {
        try {
            log.info("Attaching brand {} to product {}", brandId, productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            Brand brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            product.setBrand(brand);
            productRepository.save(product);
            return new ApiResponse<>(true, "Brand attached to product successfully", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(),
                    null, 501);
        }
    }

    public ApiResponse<Object> handleLocation(SellerBasicInfo inforequest) {

        String phone = getUserPhone();

        System.out.println("calling OSM service "
                + inforequest.latitude().getClass()
                + inforequest.longitude().getClass());

        OpenStreetMapService.AddressResponse addressDetails = openStreetMapService.getAddressFromLatLng(
                inforequest.latitude(),
                inforequest.longitude());

        System.out.println("saving address");

        Address savedAddress = saveAddress(
                addressDetails,
                phone,
                inforequest.latitude(),
                inforequest.longitude());

        if (savedAddress == null) {
            return new ApiResponse<>(false, "Location Info Not Saved", null, 500);
        }

        return new ApiResponse<>(true, "Location Info Saved", savedAddress, 200);
    }

    private Address saveAddress(AddressResponse addressDetails, String phone, BigDecimal lat, BigDecimal longi) {

        Seller seller = sellerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getOnboardingStage() == Seller.ONBOARDSTAGE.RESGISTER) {
            seller.setOnboardingStage(Seller.ONBOARDSTAGE.LOCATION);
            sellerRepository.save(seller);
        }
        Address address = sellerAddressRepository.findBySellerId(seller.getId())
                .orElseGet(Address::new);

        System.out.println("City is " + addressDetails.city());

        address.setCity(addressDetails.city());
        address.setLine1(addressDetails.line1());
        address.setState(addressDetails.state());
        address.setCountry(addressDetails.country());
        address.setPincode(addressDetails.pincode());
        address.setLatitude(lat);
        address.setLongitude(longi);
        address.setSeller(seller);

        return sellerAddressRepository.save(address);
    }
    // public ApiResponse<Object> getProductWithAttributesAndVariants(UUID
    // productId) {
    // try {
    // Product product =
    // productRepository.findProductWithAttributesAndVariants(productId)
    // .orElseThrow(() -> new RuntimeException("Invalid productId: " + productId));
    // List<ProductAttributeResponseDto> attributesDto =
    // product.getProductAttributes().stream()
    // .map(attr -> new ProductAttributeResponseDto(
    // attr.getId(),
    // attr.getCategory_attribute().getCategory().getName(), // assuming you want
    // category name
    // attr.getValue(), // adjust if you store
    // attr.getVariants().stream()
    // .map(variant -> new ProductVariantResponseDto(
    // variant.getId(),
    // variant.getSku(),
    // variant.getPrice(),
    // variant.getStock()))
    // .toList()))
    // .toList();

    // ProductFullResponseDto responseDto = new ProductFullResponseDto(
    // product.getId(),
    // product.getName(),
    // product.getDescription(),
    // attributesDto);

    // return new ApiResponse<>(true, "Product details fetched successfully",
    // responseDto, 200);

    // } catch (Exception e) {
    // return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(),
    // null, 500);
    // }
    // }

    public ApiResponse<Object> MakeProductLive(UUID productId) {
        try {
            Product.Step currentStep = productRepository.findStepById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (currentStep == Product.Step.PRODUCT_VARIANT) {
                int updatescore = updateStatusById(productId, Product.Step.LIVE);
                if (updatescore > 0) {
                    refreshSnapshot(productId);
                    handleProductUpdate(productId);
                    eventPublisher.publishProductLive(productId); // triggers search-intent indexing
                    return new ApiResponse<>(true, "Product Live", null, 200);
                } else {
                    return new ApiResponse<>(false, "Internal Server Error", null, 500);
                }

            } else
                return new ApiResponse<>(false, "Bad Request", null, 403);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 500);
        }
    }

    private void refreshSnapshot(UUID productId) {
        String snapshot = productRepository.getProductDetailAsJson(productId);
        if (snapshot != null) {
            productRepository.updateSnapshot(productId, snapshot);
        }
    }

    private int updateStatusById(UUID productId, Product.Step step) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStep(step);
        productRepository.save(product); // 🔥 will trigger @PostUpdate
        return 1;
    }

    @Async
    public void handleProductUpdate(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (Boolean.TRUE.equals(product.getIsStandard()) && product.getStep() == Product.Step.LIVE) {
            try {
                log.info("product value" + product.getName() + " " + product.getDescription() + " "
                        + product.getCategory() + " " + product.getBrand());
                StandardProduct standardProduct = new StandardProduct();
                standardProduct.setName(product.getName());
                standardProduct.setDescription(product.getDescription());
                standardProduct.setCategory(product.getCategory());
                standardProduct.setBrandEntity(product.getBrand());

                StandardProduct saved = standardProductRepository.save(standardProduct);
                log.info("StandardProduct saved: " + saved);

            } catch (Exception e) {
                log.error("Failed to save StandardProduct: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            log.info("Product not standard/live, skipping StandardProduct creation.");
        }
    }

    public ApiResponse<Object> uploadAndUpdateImages(List<Object[]> attributeImageData, String step) {
        try {
            System.out.println("Received attributeImageData: " + attributeImageData.size() + " entries");
            for (Object[] row : attributeImageData) {
                UUID productAttributeId = UUID.fromString(row[0].toString());
                List<MultipartFile> files = (List<MultipartFile>) row[1];

                // upload each image
                List<String> uploadedUrls = new ArrayList<>();
                for (MultipartFile file : files) {
                    try {
                        Map uploadResult = cloudinary.uploader()
                                .upload(file.getBytes(), ObjectUtils.emptyMap());
                        uploadedUrls.add(uploadResult.get("url").toString());
                    } catch (IOException e) {
                        return new ApiResponse<>(false,
                                "Failed to upload image for productAttributeId: " + productAttributeId,
                                null,
                                500);
                    }
                }

                // fetch product attribute
                ProductAttribute attribute = productAttributeRepository.findById(productAttributeId)
                        .orElseThrow(() -> new RuntimeException("ProductAttribute not found: " + productAttributeId));

                // update images list
                attribute.getImages().addAll(uploadedUrls);
                productAttributeRepository.save(attribute);
            }

            return new ApiResponse<>(true, "Images uploaded successfully", null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Error while uploading images: " + e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> searchProducts(String keyword) throws IOException {
        SearchResponse<Map> response = elasticsearchClient.search(s -> s
                .index("products")
                .query(q -> q
                        .bool(b -> b
                                // name
                                .should(sh -> sh.match(m -> m
                                        .field("name")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("name.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // description
                                .should(sh -> sh.match(m -> m
                                        .field("description")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("description.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // sellerName
                                .should(sh -> sh.match(m -> m
                                        .field("sellerName")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("sellerName.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // categoryName
                                .should(sh -> sh.match(m -> m
                                        .field("categoryName")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("categoryName.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // brandName
                                .should(sh -> sh.match(m -> m
                                        .field("brandName")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("brandName.keyword")
                                        .value("*" + keyword.toLowerCase() + "*"))))),
                Map.class);

        List<Map<String, Object>> result = response.hits().hits()
                .stream()
                .map(hit -> (Map<String, Object>) hit.source())
                .collect(Collectors.toList());

        return new ApiResponse<>(true, "result", result, 200);
    }

    public ApiResponse<Object> getShopCategories() {
        try {
            // List<Seller.ShopCategory> categories =
            // sellerRepository.findAllShopCategories();
            return new ApiResponse<>(true, "Shop Categories fetched", "categories", 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> searchShop(String keyword) {
        try {
            // List<Seller.ShopCategory> categories =
            // sellerRepository.findAllShopCategories();
            return new ApiResponse<>(true, "Shop Categories fetched", "categories", 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> getShopsByCity(String city) {
        try {
            List<Seller> shops = sellerRepository.findByAddress_City(city);
            return new ApiResponse<>(true, "Shops fetched by city", shops, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> getShopsByCityAndCategory(String city, Category category) {
        // List<Seller> shops = sellerRepository.findByAddress_CityAndShopCategory(city,
        // category);
        return new ApiResponse<>(true, "Shops fetched by city and category", "shops", 200);
    }

    public ApiResponse<Object> getNearestShops(double lat, double lon, int limit) {
        List<Seller> shops = sellerRepository.findNearestShops(lat, lon, limit);
        return new ApiResponse<>(true, "Nearest shops fetched", shops, 200);
    }

    public ApiResponse<Object> getNearestShopsByCategory(double lat, double lon, Category category,
            int limit) {
        try {
            // List<Seller> shops = sellerRepository.findNearestShopsByCategory(lat, lon,
            // category.name(), limit);
            return new ApiResponse<>(true, "Nearest shops by category fetched", "shops", 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    private UUID getUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    private String getUserPhone() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getPhone();
    }
}
// hukiiu iuui jkjbhjhhjhj huhu uhh,j uh yiu ujhhuhjuhui uhh juyyuuik uhhu
// jliij oijij uhjinhuk hkuhj uhiu uhiuhuhkui uihhjhhhijji hukhu guy uy7 yuyu
// yuuyy yuyu