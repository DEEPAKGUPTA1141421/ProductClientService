package com.ProductClientService.ProductClientService.Service.seller;

import com.ProductClientService.ProductClientService.Configuration.ElasticsearchIndexInitializer;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Seeds a minimal StandardProduct catalog on first boot.
 * Runs only when the standard_products table is empty, so it is safe to
 * redeploy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StandardProductSeeder {

        private final StandardProductRepository standardProductRepository;
        private final StandardProductIndexer standardProductIndexer;
        private final ElasticsearchIndexInitializer indexInitializer;

        @PersistenceContext
        private EntityManager em;

        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void seed() {
                long dbCount = standardProductRepository.count();

                if (dbCount > 0) {
                        // DB already populated — check whether ES also has documents.
                        // Re-index if ES was cleared (e.g. fresh cluster restart).
                        if (!indexInitializer.catalogIndexHasDocuments()) {
                                log.info("Catalog DB has {} entries but ES is empty — re-indexing…", dbCount);
                                standardProductIndexer.bulkIndex(standardProductRepository.findAllWithAssociations());
                        } else {
                                log.info("StandardProduct catalog already seeded in DB and ES — skipping.");
                        }
                        return;
                }
                log.info("Seeding StandardProduct catalog…");

                // ── Categories (SUPER_CATEGORY → CATEGORY → SUBCATEGORY) ─────────────
                Category electronics = findOrCreateCategory("Electronics", Category.Level.CATEGORY, null);
                Category smartphones = findOrCreateCategory("Smartphones", Category.Level.SUBCATEGORY, electronics);

                Category grocery = findOrCreateCategory("Grocery & Food", Category.Level.CATEGORY, null);
                Category instantFood = findOrCreateCategory("Instant Food", Category.Level.SUBCATEGORY, grocery);
                Category dairy = findOrCreateCategory("Dairy & Eggs", Category.Level.SUBCATEGORY, grocery);

                Category personalCare = findOrCreateCategory("Personal Care", Category.Level.CATEGORY, null);
                Category bodyCare = findOrCreateCategory("Soap & Body Wash", Category.Level.SUBCATEGORY, personalCare);
                Category hairCare = findOrCreateCategory("Hair Care", Category.Level.SUBCATEGORY, personalCare);

                Category homeKitchen = findOrCreateCategory("Home & Kitchen", Category.Level.CATEGORY, null);
                Category cookware = findOrCreateCategory("Cookware", Category.Level.SUBCATEGORY, homeKitchen);
                Category flasks = findOrCreateCategory("Bottles & Flasks", Category.Level.SUBCATEGORY, homeKitchen);

                // ── Brands ────────────────────────────────────────────────────────────
                Brand samsung = findOrCreateBrand("Samsung", "samsung", smartphones.getId());
                Brand apple = findOrCreateBrand("Apple", "apple", smartphones.getId());
                Brand oneplus = findOrCreateBrand("OnePlus", "oneplus", smartphones.getId());
                Brand boat = findOrCreateBrand("boAt", "boat", smartphones.getId());
                Brand nestle = findOrCreateBrand("Nestlé", "nestle", instantFood.getId());
                Brand amul = findOrCreateBrand("Amul", "amul", dairy.getId());
                Brand tata = findOrCreateBrand("Tata", "tata", dairy.getId());
                Brand dove = findOrCreateBrand("Dove", "dove", bodyCare.getId());
                Brand hul = findOrCreateBrand("Head & Shoulders", "headshoulders", hairCare.getId());
                Brand prestige = findOrCreateBrand("Prestige", "prestige", cookware.getId());
                Brand milton = findOrCreateBrand("Milton", "milton", flasks.getId());

                // ── Electronics / Smartphones ─────────────────────────────────────────
                save("Samsung Galaxy M51",
                                "6000 mAh battery with 25W fast charging. 6.7\" FHD+ Super AMOLED display. " +
                                                "64 MP quad camera with OIS. Snapdragon 730G processor.",
                                "8901212270215", "SAM-GALAXY-M51",
                                "samsung,galaxy,m51,6000mah,super amoled,snapdragon 730g,android",
                                "https://images.samsung.com/in/smartphones/galaxy-m51/images/galaxy-m51-front.jpg",
                                samsung, smartphones);

                save("Samsung Galaxy A54 5G",
                                "50 MP OIS triple camera. 5000 mAh battery. Gorilla Glass 5. " +
                                                "Exynos 1380 processor. IP67 water resistance.",
                                "8901212559876", "SAM-GALAXY-A54-5G",
                                "samsung,galaxy,a54,5g,50mp,gorilla glass,ip67,exynos",
                                "https://images.samsung.com/in/smartphones/galaxy-a54/images/galaxy-a54-5g-front.jpg",
                                samsung, smartphones);

                save("Apple iPhone 14",
                                "A15 Bionic chip. 12 MP dual-camera system with Photonic Engine. " +
                                                "Crash Detection. Emergency SOS via satellite. All-day battery life.",
                                "0194253082842", "APL-IPHONE14-128",
                                "apple,iphone,14,ios,a15 bionic,crash detection,photonic engine",
                                "https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-14-black-select-2022",
                                apple, smartphones);

                save("Apple iPhone 15",
                                "A16 Bionic chip. 48 MP main camera. Dynamic Island. USB-C connector. " +
                                                "Color-infused back glass and contoured edges.",
                                "0194253954483", "APL-IPHONE15-128",
                                "apple,iphone,15,ios,a16 bionic,dynamic island,48mp,usb-c",
                                "https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-black-select-2023",
                                apple, smartphones);

                save("OnePlus Nord CE 3 Lite 5G",
                                "108 MP primary camera. Snapdragon 695 5G processor. 5000 mAh battery " +
                                                "with 67W SUPERVOOC charging. 6.72\" LCD display.",
                                "8904130180271", "OP-NORDCE3L-5G",
                                "oneplus,nord,ce3 lite,5g,108mp,snapdragon 695,67w supervooc",
                                "",
                                oneplus, smartphones);

                save("boAt Airdopes 141 TWS",
                                "42H total playback. ENx™ technology for calls. BEAST™ Mode for low latency gaming. " +
                                                "IPX4 water resistance. Bluetooth 5.1.",
                                "8906060720712", "BOAT-AIRDOPES-141",
                                "boat,airdopes,141,tws,earbuds,wireless,42h,gaming,ipx4,bluetooth",
                                "",
                                boat, smartphones);

                // ── Grocery / Instant Food ────────────────────────────────────────────
                save("Maggi 2-Minute Masala Noodles 70g",
                                "Classic instant noodles ready in 2 minutes. Made with real vegetables " +
                                                "and a special masala tastemaker. No added MSG.",
                                "8901058852429", "MAGGI-MASALA-70G",
                                "maggi,noodles,instant,masala,2 minute,fast food,snack",
                                "https://www.maggi.in/sites/default/files/2023-01/masala-noodles.jpg",
                                nestle, instantFood);

                save("Maggi 2-Minute Atta Noodles 80g",
                                "Made with whole wheat (atta). Same great 2-minute taste with added fibre. " +
                                                "No added MSG.",
                                "8901058875008", "MAGGI-ATTA-80G",
                                "maggi,noodles,instant,atta,whole wheat,healthy,fast food",
                                "",
                                nestle, instantFood);

                // ── Grocery / Dairy ───────────────────────────────────────────────────
                save("Amul Butter 500g",
                                "Made from pasteurised cream. Rich, natural dairy flavour. Hygienically packed. " +
                                                "No added preservatives.",
                                "8901052012345", "AMUL-BUTTER-500G",
                                "amul,butter,dairy,cream,500g,pasteurised",
                                "https://www.amul.com/files/images/products/amul-butter-500g.jpg",
                                amul, dairy);

                save("Tata Salt Lite 1kg",
                                "Low sodium salt with iodine. 15% less sodium than regular salt. " +
                                                "Ideal for health-conscious households.",
                                "8901272001234", "TATA-SALT-LITE-1KG",
                                "tata,salt,low sodium,iodized,1kg,healthy",
                                "",
                                tata, dairy);

                // ── Personal Care / Body ──────────────────────────────────────────────
                save("Dove Cream Beauty Bathing Bar 100g",
                                "With 1/4 moisturising cream. Soap-free formula that is gentle on skin. " +
                                                "Leaves skin soft and smooth after every wash.",
                                "8901030869793", "DOVE-CREAM-BAR-100G",
                                "dove,soap,bathing bar,moisturising,cream,100g,gentle",
                                "https://www.dove.com/content/dam/dove/in/dove-bathing-bar-100g.jpg",
                                dove, bodyCare);

                // ── Personal Care / Hair ──────────────────────────────────────────────
                save("Head & Shoulders Cool Menthol Shampoo 340ml",
                                "Removes up to 100% of visible dandruff. Refreshing cool menthol formula. " +
                                                "pH balanced. Dermatologist tested.",
                                "8901030654701", "HS-COOL-MENTHOL-340ML",
                                "head and shoulders,shampoo,dandruff,cool menthol,340ml,dermatologist tested",
                                "",
                                hul, hairCare);

                // ── Home & Kitchen / Cookware ─────────────────────────────────────────
                save("Prestige Aluminium Pressure Cooker 5L",
                                "Alpha base technology for even heat distribution. Gasket release system for safety. " +
                                                "ISI certified. Compatible with induction and gas stoves.",
                                "8901088001234", "PRESTIGE-PC-ALU-5L",
                                "prestige,pressure cooker,aluminium,5 litre,isi certified,induction compatible",
                                "",
                                prestige, cookware);

                save("Prestige Hard Anodised Kadai 3L with Glass Lid",
                                "Hard anodised for superior non-stick and long life. " +
                                                "Ergonomic handles. Tempered glass lid. Induction base.",
                                "8901088009873", "PRESTIGE-KADAI-HA-3L",
                                "prestige,kadai,hard anodised,non stick,3 litre,induction,glass lid",
                                "",
                                prestige, cookware);

                // ── Home & Kitchen / Flasks ───────────────────────────────────────────
                save("Milton Thermosteel Flip Lid Flask 500ml",
                                "Keeps beverages hot for 24 hours, cold for 12 hours. " +
                                                "Double-wall vacuum insulation. Food-grade stainless steel. Leak-proof.",
                                "8901099012345", "MILTON-THERMO-FLIP-500ML",
                                "milton,flask,thermosteel,500ml,insulated,hot cold,stainless steel,leak proof",
                                "",
                                milton, flasks);

                save("Milton Steel Fridge Water Bottle 1 Litre",
                                "100% food-grade stainless steel. Fit for regular fridge use. " +
                                                "Rust-proof. BPA-free. Easy-grip design.",
                                "8901099067832", "MILTON-STEEL-FRIDGE-1L",
                                "milton,bottle,steel,1 litre,fridge,bpa free,rust proof",
                                "",
                                milton, flasks);

                long total = standardProductRepository.count();
                log.info("StandardProduct catalog seeded — {} DB entries added.", total);

                // Index everything to Elasticsearch so catalog search goes through ES,
                // not the slower LIKE-query fallback.
                log.info("Indexing {} catalog entries to Elasticsearch (catalog-v1)…", total);
                standardProductIndexer.bulkIndex(standardProductRepository.findAllWithAssociations());
        }

        // ── helpers ───────────────────────────────────────────────────────────────

        private Category findOrCreateCategory(String name, Category.Level level, Category parent) {
                List<Category> found = em.createQuery(
                                "SELECT c FROM Category c WHERE LOWER(c.name) = :n", Category.class)
                                .setParameter("n", name.toLowerCase())
                                .getResultList();
                if (!found.isEmpty())
                        return found.get(0);

                Category c = new Category();
                c.setName(name);
                c.setCategoryLevel(level);
                if (parent != null)
                        c.setParent(parent);
                em.persist(c);
                em.flush();
                return c;
        }

        private Brand findOrCreateBrand(String name, String normalisedName, UUID categoryId) {
                List<Brand> found = em.createQuery(
                                "SELECT b FROM Brand b WHERE b.normalisedName = :n AND b.categoryId = :cat",
                                Brand.class)
                                .setParameter("n", normalisedName)
                                .setParameter("cat", categoryId)
                                .getResultList();
                if (!found.isEmpty())
                        return found.get(0);

                Brand b = new Brand();
                b.setName(name);
                b.setNormalisedName(normalisedName);
                b.setCategoryId(categoryId);
                b.setApproved(true);
                b.setActive(true);
                em.persist(b);
                em.flush();
                return b;
        }

        private void save(String name, String description, String ean, String productCode,
                        String keywords, String imageUrl, Brand brand, Category category) {
                if (standardProductRepository.existsByProductCode(productCode))
                        return;
                if (ean != null && standardProductRepository.existsByEan(ean))
                        return;

                StandardProduct sp = new StandardProduct();
                sp.setName(name);
                sp.setDescription(description);
                sp.setEan(ean);
                sp.setProductCode(productCode);
                sp.setSearchKeywords(keywords);
                sp.setPrimaryImageUrl(imageUrl);
                sp.setBrandEntity(brand);
                sp.setCategory(category);
                sp.setIsVerified(true);
                sp.setStatus(StandardProduct.Status.ACTIVE);
                standardProductRepository.save(sp);
        }
}
// kikik jklkljkbnjkjnjkknjnjjn bhhhkhvgghjhmjhjhjbbh