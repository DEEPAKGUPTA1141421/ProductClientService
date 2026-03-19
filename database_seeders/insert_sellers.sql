-- Script to insert 200 sellers across different categories with real-world data
-- Run this script directly in your PostgreSQL database

-- Category IDs from the API response
WITH categories AS (
    SELECT '0b51af17-cea5-411d-b0b2-d1e86b35c8b0' as cat_id, 'Men Fashion' as cat_name UNION ALL
    SELECT '6b525380-eed4-4bb3-b688-5840e49296ee', 'Women Fashion' UNION ALL
    SELECT '1660f3b3-e366-4561-a479-7b9fc9f3ac26', 'Home & Living' UNION ALL
    SELECT '5c46c5f5-04b9-47c0-b20c-3bd108a72c14', 'Kids & Toys' UNION ALL
    SELECT '28766db2-a367-41b3-b19c-30be257bc7ef', 'Personal Care & Wellness' UNION ALL
    SELECT '5d70fc95-8a6b-4d04-95e9-9620269ab15e', 'Mobiles & Tablets' UNION ALL
    SELECT '3f6bf59e-66e6-4cd3-abdb-2780f608f052', 'Consumer Electronics' UNION ALL
    SELECT '91c0ef20-1199-48a8-bec8-208e5d04b15e', 'Appliances' UNION ALL
    SELECT 'b35ecf36-4fa3-41d0-8eef-98bf293690b2', 'Automotive' UNION ALL
    SELECT 'f7aeabba-0dd4-4545-b855-402d58d04e85', 'Beauty & Personal Care' UNION ALL
    SELECT '623bfe8d-ad47-4b98-9db2-db5c15c251dd', 'Home Utility' UNION ALL
    SELECT '5eb10384-7174-42b5-9364-118980acbea2', 'Kids' UNION ALL
    SELECT 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db', 'Grocery' UNION ALL
    SELECT 'eea1b493-7bfe-44e3-92fd-8c81dd370c39', 'Women' UNION ALL
    SELECT 'da393bb8-af72-435b-b470-326220c4198b', 'Home & Kitchen' UNION ALL
    SELECT '1c5aeb0a-5a17-41c9-baac-0d7280483c06', 'Health & Wellness' UNION ALL
    SELECT 'eba3b291-ffae-415e-a5cf-d03ce85bc79b', 'Beauty & Makeup' UNION ALL
    SELECT 'a9c7c17f-e0a9-4329-a351-cecf6dc14add', 'Personal Care' UNION ALL
    SELECT 'cd0bf961-8d05-4ee3-9ee5-82e26b99d295', 'Men\'S Grooming' UNION ALL
    SELECT '828f7071-d985-434e-aac1-38671e7c85a5', 'Craft & Office Supplies' UNION ALL
    SELECT 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24', 'Sports & Fitness' UNION ALL
    SELECT 'ef990cb0-1782-4c75-a6f6-95dd7189ad28', 'Automotive Accessories' UNION ALL
    SELECT '9098b0a0-0bb1-48fe-b50e-f2e349877cee', 'Pet Supplies' UNION ALL
    SELECT 'ccb4ee59-a447-40d4-8489-915414b59eb6', 'Office Supplies & Stationery' UNION ALL
    SELECT '3862aea4-c59a-49f4-893a-b1ca29438f52', 'Industrial & Scientific Products' UNION ALL
    SELECT '597196ef-fef5-4249-a01d-51cc66cff579', 'Musical Instruments' UNION ALL
    SELECT 'f87d53a4-9198-4678-b9e2-a7e42894fe5b', 'Books' UNION ALL
    SELECT 'b32c5e70-79dd-4991-87f7-6f0588e1852b', 'Eye Utility' UNION ALL
    SELECT 'c2cb4055-f18a-4316-b906-a9b5df07e07d', 'Bags, Luggage & Travel Accessories' UNION ALL
    SELECT 'e0cd73a3-4fcb-4f5e-9482-295b6ebf1e45', 'Mens Personal Care & Grooming'
),
shop_data AS (
    -- Electronics & Appliances
    SELECT 1 as shop_num, 'LG India', 'https://www.lg.com/in/images/logo.png', '9876543210', '3f6bf59e-66e6-4cd3-abdb-2780f608f052' UNION ALL
    SELECT 2, 'Samsung India Store', 'https://www.samsung.com/content/dam/samsungmobile/en/ui/galaxy-a/insights/index-image-1.jpg', '9876543211', '3f6bf59e-66e6-4cd3-abdb-2780f608f052' UNION ALL
    SELECT 3, 'Sony Electronics India', 'https://www.sony.co.in/images/common/sony-logo.jpg', '9876543212', '3f6bf59e-66e6-4cd3-abdb-2780f608f052' UNION ALL
    SELECT 4, 'Philips India', 'https://www.philips.co.in/c-dam/b2c/master/experience/brands/philips-logo-2021.png', '9876543213', '91c0ef20-1199-48a8-bec8-208e5d04b15e' UNION ALL
    SELECT 5, 'Midea Appliances', 'https://www.midea.com/globalAssets/logo.png', '9876543214', '91c0ef20-1199-48a8-bec8-208e5d04b15e' UNION ALL
    SELECT 6, 'IFB Industries', 'https://www.ifbappliances.com/logo.png', '9876543215', '91c0ef20-1199-48a8-bec8-208e5d04b15e' UNION ALL
    SELECT 7, 'Whirlpool India', 'https://www.whirlpoolindia.com/logo.jpg', '9876543216', '91c0ef20-1199-48a8-bec8-208e5d04b15e' UNION ALL
    SELECT 8, 'Voltas Limited', 'https://www.voltas.com/images/logos/voltas-logo.png', '9876543217', '91c0ef20-1199-48a8-bec8-208e5d04b15e' UNION ALL
    SELECT 9, 'Haier Appliances India', 'https://www.haier.co.in/static/images/logo.png', '9876543218', '91c0ef20-1199-48a8-bec8-208e5d04b15e' UNION ALL
    SELECT 10, 'Godrej & Boyce', 'https://www.godrej.com/images/godrej-logo.png', '9876543219', '91c0ef20-1199-48a8-bec8-208e5d04b15e' UNION ALL
    
    -- Fashion & Apparel
    SELECT 11, 'H&M India', 'https://www2.hm.com/hmgobjective/banners/2022w36/hm_logo.jpg', '9876543220', '0b51af17-cea5-411d-b0b2-d1e86b35c8b0' UNION ALL
    SELECT 12, 'Zara India', 'https://www.zara.com/static/images/logo-zara.svg', '9876543221', '0b51af17-cea5-411d-b0b2-d1e86b35c8b0' UNION ALL
    SELECT 13, 'Forever 21 India', 'https://www.forever21.com/images/logo.png', '9876543222', '6b525380-eed4-4bb3-b688-5840e49296ee' UNION ALL
    SELECT 14, 'ASOS India', 'https://www.asos.com/static/images/logo.svg', '9876543223', '6b525380-eed4-4bb3-b688-5840e49296ee' UNION ALL
    SELECT 15, 'Uniqlo India', 'https://www.uniqlo.com/common/images/header/logo.gif', '9876543224', '0b51af17-cea5-411d-b0b2-d1e86b35c8b0' UNION ALL
    SELECT 16, 'Diesel India', 'https://www.diesel.com/images/logo.png', '9876543225', '0b51af17-cea5-411d-b0b2-d1e86b35c8b0' UNION ALL
    SELECT 17, 'Levis Official Store', 'https://www.levis.com/images/logo.gif', '9876543226', '0b51af17-cea5-411d-b0b2-d1e86b35c8b0' UNION ALL
    SELECT 18, 'Calvin Klein India', 'https://www.calvinklein.co.in/logo.png', '9876543227', '6b525380-eed4-4bb3-b688-5840e49296ee' UNION ALL
    SELECT 19, 'Tommy Hilfiger India', 'https://www.tommyhilfiger.co.in/logo.svg', '9876543228', '0b51af17-cea5-411d-b0b2-d1e86b35c8b0' UNION ALL
    SELECT 20, 'Puma India', 'https://www.puma.com/en/about/img/puma-logo.svg', '9876543229', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    
    -- Beauty & Personal Care
    SELECT 21, 'MAC Cosmetics India', 'https://www.maccosmetics.in/logo.png', '9876543230', 'eba3b291-ffae-415e-a5cf-d03ce85bc79b' UNION ALL
    SELECT 22, 'Lakme Beauty', 'https://www.lakmeindia.com/images/logo.png', '9876543231', 'eba3b291-ffae-415e-a5cf-d03ce85bc79b' UNION ALL
    SELECT 23, 'L\'Oreal Paris India', 'https://www.loreal.co.in/logo.png', '9876543232', 'eba3b291-ffae-415e-a5cf-d03ce85bc79b' UNION ALL
    SELECT 24, 'The Body Shop India', 'https://www.thebodyshop.com/images/logo.png', '9876543233', 'f7aeabba-0dd4-4545-b855-402d58d04e85' UNION ALL
    SELECT 25, 'Decathlon India', 'https://www.decathlon.in/images/logo.png', '9876543234', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 26, 'Bobbi Brown India', 'https://www.bobbibrown.in/logo.png', '9876543235', 'eba3b291-ffae-415e-a5cf-d03ce85bc79b' UNION ALL
    SELECT 27, 'Revlon India', 'https://www.revlon.in/logo.png', '9876543236', 'eba3b291-ffae-415e-a5cf-d03ce85bc79b' UNION ALL
    SELECT 28, 'Maybelline India', 'https://www.maybelline.co.in/logo.png', '9876543237', 'eba3b291-ffae-415e-a5cf-d03ce85bc79b' UNION ALL
    SELECT 29, 'Oriflame India', 'https://www.orflame.co.in/static/logo.png', '9876543238', 'f7aeabba-0dd4-4545-b855-402d58d04e85' UNION ALL
    SELECT 30, 'Avon India', 'https://www.avon.in/static/images/avon-logo.png', '9876543239', 'f7aeabba-0dd4-4545-b855-402d58d04e85' UNION ALL
    
    -- Grocery & Food
    SELECT 31, 'BigBasket Seller', 'https://www.bigbasket.com/images/logo.png', '9876543240', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 32, 'FreshKart Merchants', 'https://www.freshkart.com/logo.svg', '9876543241', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 33, 'Organic Valley India', 'https://www.organicvalley.in/logo.png', '9876543242', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 34, 'Mother Dairy Store', 'https://www.motherdairy.com/images/logo.png', '9876543243', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 35, 'Amul Dairy', 'https://www.amul.com/images/logo.gif', '9876543244', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 36, 'ITC Foods', 'https://www.itcportal.com/logo.png', '9876543245', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 37, 'Britannia Industries', 'https://www.britannia.co.in/images/logo.png', '9876543246', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 38, 'Nestlé India', 'https://www.nestle.co.in/logo.png', '9876543247', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 39, 'Patanjali Ayurved', 'https://www.patanjaliayurveda.com/logo.png', '9876543248', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    SELECT 40, 'Baidyanath Ayurved', 'https://www.baidyanath.co.in/logo.png', '9876543249', 'b6b5e44d-37bb-4ef9-9b77-26fa8e3836db' UNION ALL
    
    -- Mobiles & Tablets
    SELECT 41, 'OnePlus Official', 'https://www.oneplus.in/images/logo.png', '9876543250', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 42, 'Xiaomi India', 'https://www.mi.com/in/images/logo.png', '9876543251', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 43, 'Realme India', 'https://www.realme.com/in/logo.png', '9876543252', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 44, 'Apple Store India', 'https://www.apple.com/in/images/logo.gif', '9876543253', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 45, 'OPPO India', 'https://www.oppomobile.in/logo.png', '9876543254', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 46, 'Vivo India', 'https://www.vivo.co.in/logo.png', '9876543255', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 47, 'Motorola India', 'https://www.motorola.co.in/logo.png', '9876543256', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 48, 'Nokia Mobile India', 'https://www.nokia.com/phones/en_in/logo.png', '9876543257', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 49, 'Nothing Phone', 'https://www.nothing.tech/in/logo.png', '9876543258', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    SELECT 50, 'Honor India', 'https://www.honor.com/in/logo.png', '9876543259', '5d70fc95-8a6b-4d04-95e9-9620269ab15e' UNION ALL
    
    -- Home & Living
    SELECT 51, 'IKEA India', 'https://www.ikea.com/images/ikea-logo.svg', '9876543260', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 52, 'Godrej Interio', 'https://www.godrejinterio.com/logo.png', '9876543261', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 53, 'Urban Ladder', 'https://www.urbanladder.com/images/logo.png', '9876543262', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 54, 'Flipkart Decor', 'https://www.flipkart.com/images/logo.png', '9876543263', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 55, 'Amazon Home Store', 'https://www.amazon.in/images/logo.png', '9876543264', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 56, 'Pepperfry', 'https://www.pepperfry.com/logo.svg', '9876543265', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 57, 'HomeTown', 'https://www.hometown.in/logo.png', '9876543266', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 58, 'Fabfurnish', 'https://www.fabfurnish.com/logo.svg', '9876543267', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 59, 'Nilkamal Furniture', 'https://www.nilkamal.com/logo.png', '9876543268', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    SELECT 60, 'ID Lifestyle', 'https://www.id-lifestyle.com/logo.png', '9876543269', '1660f3b3-e366-4561-a479-7b9fc9f3ac26' UNION ALL
    
    -- Kids & Toys
    SELECT 61, 'Lego India', 'https://www.lego.com/images/logo.svg', '9876543270', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 62, 'Fisher-Price India', 'https://www.fisher-price.in/logo.png', '9876543271', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 63, 'Funskool', 'https://www.funskool.co.in/logo.svg', '9876543272', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 64, 'Hasbro India', 'https://www.hasbro.com/images/logo.png', '9876543273', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 65, 'Mattel India', 'https://www.mattel.com/images/logo.svg', '9876543274', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 66, 'Playskool by Funskool', 'https://www.funskool.co.in/logo.svg', '9876543275', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 67, 'VTech India', 'https://www.vtechkids.com/images/logo.png', '9876543276', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 68, 'KidoTech Toys', 'https://www.kidotech.in/logo.png', '9876543277', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 69, 'Baby Kingdom', 'https://www.babykingdom.in/logo.svg', '9876543278', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    SELECT 70, 'Pigeon Baby Care', 'https://www.pigeon.in/logo.png', '9876543279', '5c46c5f5-04b9-47c0-b20c-3bd108a72c14' UNION ALL
    
    -- Automotive
    SELECT 71, 'Maruti Suzuki', 'https://www.marutisuzuki.com/logo.png', '9876543280', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 72, 'Hyundai India', 'https://www.hyundai.co.in/logo.png', '9876543281', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 73, 'Tata Motors', 'https://www.tatamotors.com/logo.png', '9876543282', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 74, 'Mahindra & Mahindra', 'https://www.mahindra.com/logo.png', '9876543283', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 75, 'Bajaj Auto', 'https://www.bajajauto.com/logo.png', '9876543284', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 76, 'Hero MotoCorp', 'https://www.heromotocorp.com/logo.png', '9876543285', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 77, 'Standard Motor Company', 'https://www.standardmotors.in/logo.png', '9876543286', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 78, 'Harley-Davidson India', 'https://www.harley-davidson.com/images/logo.svg', '9876543287', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 79, 'Royal Enfield', 'https://www.royalenfield.com/logo.png', '9876543288', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    SELECT 80, 'TVS Motor', 'https://www.tvsmotors.com/logo.png', '9876543289', 'b35ecf36-4fa3-41d0-8eef-98bf293690b2' UNION ALL
    
    -- Sports & Fitness
    SELECT 81, 'Nike India', 'https://www.nike.com/images/logo.svg', '9876543290', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 82, 'Adidas India', 'https://www.adidas.co.in/logo.svg', '9876543291', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 83, 'Reebok India', 'https://www.reebok.co.in/logo.png', '9876543292', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 84, 'Skechers India', 'https://www.skechers.com/images/logo.svg', '9876543293', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 85, 'Under Armour India', 'https://www.underarmour.co.in/logo.png', '9876543294', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 86, 'Decathlon Sports', 'https://www.decathlon.in/logo.png', '9876543295', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 87, 'Domyos Fitness', 'https://www.domyos.co.in/logo.png', '9876543296', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 88, 'Cosco Sports', 'https://www.coscoglobal.com/logo.png', '9876543297', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 89, 'Yonex Sports', 'https://www.yonex.com/images/logo.png', '9876543298', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    SELECT 90, 'Badminton Warehouse India', 'https://www.badmintonwarehouse.in/logo.svg', '9876543299', 'f6beded0-0ebb-414c-9ec2-a7415a5a5e24' UNION ALL
    
    -- Books
    SELECT 91, 'Amazon Books India', 'https://www.amazon.in/images/logo.png', '9876543300', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 92, 'Flipkart Books', 'https://www.flipkart.com/images/logo.png', '9876543301', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 93, 'Sapna Book House', 'https://www.sapnaonline.com/logo.png', '9876543302', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 94, 'Oxford Bookstore', 'https://www.oxfordbookstore.com/logo.png', '9876543303', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 95, 'Crossword Bookstores', 'https://www.crosswordstore.com/logo.svg', '9876543304', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 96, 'DC Books Online', 'https://www.dcbooks.com/logo.png', '9876543305', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 97, 'Landmark Books', 'https://www.landmarkbookstore.com/logo.svg', '9876543306', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 98, 'Notion Press', 'https://www.notionpress.com/logo.png', '9876543307', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 99, 'Penguin India', 'https://www.penguin.co.in/logo.png', '9876543308', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    SELECT 100, 'HarperCollins India', 'https://www.harpercollins.co.in/logo.svg', '9876543309', 'f87d53a4-9198-4678-b9e2-a7e42894fe5b' UNION ALL
    
    -- Continue with more categories... (remaining 100 shops)
    SELECT 101, 'Titan Watch Store', 'https://www.titannewsletters.com/logo.svg', '9876543310', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 102, 'Fastrack Eyewear', 'https://www.fastrack.in/logo.png', '9876543311', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 103, 'Prada Sunglasses', 'https://www.prada.com/images/logo.png', '9876543312', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 104, 'Ray-Ban India', 'https://www.ray-ban.com/images/logo.svg', '9876543313', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 105, 'Oakley Sunglasses', 'https://www.oakley.com/images/logo.png', '9876543314', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 106, 'Gucci Eyewear', 'https://www.gucci.com/images/logo.svg', '9876543315', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 107, 'Versace Accessories', 'https://www.versace.com/images/logo.png', '9876543316', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 108, 'Armani Exchange', 'https://www.armaniexchange.com/logo.svg', '9876543317', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 109, 'FCUK Watches', 'https://www.frenchconnection.com/logo.png', '9876543318', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    SELECT 110, 'Fossil Watches India', 'https://www.fossil.com/images/logo.png', '9876543319', 'b32c5e70-79dd-4991-87f7-6f0588e1852b' UNION ALL
    
    -- Travel Accessories
    SELECT 111, 'American Tourister', 'https://www.americantourister.co.in/logo.svg', '9876543320', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 112, 'Samsonite India', 'https://www.samsonite.co.in/logo.png', '9876543321', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 113, 'VIP Industries', 'https://www.vipind.com/logo.svg', '9876543322', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 114, 'Carlton Luggage', 'https://www.carltonluggage.co.in/logo.png', '9876543323', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 115, 'Baggit India', 'https://www.baggit.com/logo.svg', '9876543324', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 116, 'Tommy Hilfiger Bags', 'https://www.tommyhilfiger.co.in/logo.svg', '9876543325', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 117, 'Coach Bags India', 'https://www.coach.com/images/logo.svg', '9876543326', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 118, 'Michael Kors India', 'https://www.michaelkors.co.in/logo.png', '9876543327', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 119, 'Fastrack Bags', 'https://www.fastrack.in/logo.png', '9876543328', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    SELECT 120, 'Wildcraft Adventure Store', 'https://www.wildcraft.in/logo.png', '9876543329', 'c2cb4055-f18a-4316-b906-a9b5df07e07d' UNION ALL
    
    -- Office Supplies & Stationery
    SELECT 121, 'Parker Pens India', 'https://www.parkerindia.com/logo.svg', '9876543330', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 122, 'Faber-Castell', 'https://www.faber-castell.com/images/logo.png', '9876543331', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 123, 'Cello Stationery', 'https://www.cellocorp.com/logo.svg', '9876543332', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 124, 'ITC Stationery', 'https://www.itcportal.com/logo.png', '9876543333', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 125, 'Staedtler', 'https://www.staedtler.com/images/logo.png', '9876543334', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 126, 'Rotring Writing Instruments', 'https://www.rotring.com/images/logo.svg', '9876543335', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 127, 'Pilot Pens India', 'https://www.pilot.com.in/logo.png', '9876543336', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 128, 'Camlin Art Supplies', 'https://www.camlincreatives.com/logo.svg', '9876543337', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 129, 'Luxor Writing Company', 'https://www.luxorpens.com/logo.png', '9876543338', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    SELECT 130, 'Action Stationery', 'https://www.action.com/en-in/logo.svg', '9876543339', 'ccb4ee59-a447-40d4-8489-915414b59eb6' UNION ALL
    
    -- Musical Instruments
    SELECT 131, 'Yamaha Music India', 'https://www.yamaha.co.in/logo.png', '9876543340', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 132, 'Roland Music Store', 'https://www.roland.com/images/logo.svg', '9876543341', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 133, 'Fender Guitars India', 'https://www.fender.com/images/logo.png', '9876543342', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 134, 'Gibson Guitars', 'https://www.gibson.com/images/logo.svg', '9876543343', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 135, 'Ibanez Guitars India', 'https://www.ibanez.com/images/logo.png', '9876543344', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 136, 'Korg Synthesizers', 'https://www.korg.com/images/logo.svg', '9876543345', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 137, 'VOX Amplifiers', 'https://www.voxamps.com/images/logo.png', '9876543346', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 138, 'Bose Audio Systems', 'https://www.bose.co.in/logo.svg', '9876543347', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 139, 'JBL Audio India', 'https://www.jbl.in/logo.png', '9876543348', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    SELECT 140, 'Sennheiser Audio Store', 'https://www.sennheiser.com/images/logo.svg', '9876543349', '597196ef-fef5-4249-a01d-51cc66cff579' UNION ALL
    
    -- Pet Supplies
    SELECT 141, 'Pedigree Pet Food', 'https://www.pedigree.co.in/logo.svg', '9876543350', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 142, 'Whiskas Cat Food', 'https://www.whiskas.co.in/logo.png', '9876543351', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 143, 'Royal Canin Pet Nutrition', 'https://www.royalcanin.com/images/logo.svg', '9876543352', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 144, 'Purina Pet Care', 'https://www.purina.com/images/logo.png', '9876543353', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 145, 'Hill\'s Pet Nutrition', 'https://www.hillspet.com/images/logo.svg', '9876543354', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 146, 'IAMS Pet Food', 'https://www.iams.com/images/logo.png', '9876543355', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 147, 'Farmina Pet Foods', 'https://www.farmina.com/images/logo.svg', '9876543356', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 148, 'Drools Pet Food', 'https://www.droolspetfood.com/logo.png', '9876543357', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 149, 'Orijen Pet Foods', 'https://www.orijen.ca/images/logo.svg', '9876543358', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    SELECT 150, 'Acana Pet Foods', 'https://www.acana.com/images/logo.png', '9876543359', '9098b0a0-0bb1-48fe-b50e-f2e349877cee' UNION ALL
    
    -- Industrial & Scientific
    SELECT 151, 'Bosch Industrial India', 'https://www.bosch-professional.com/images/logo.svg', '9876543360', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 152, 'Makita Power Tools', 'https://www.makita.co.in/logo.png', '9876543361', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 153, 'DeWalt Tools India', 'https://www.dewalt.co.in/logo.svg', '9876543362', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 154, 'Stanley Tools', 'https://www.stanleyworks.com/images/logo.png', '9876543363', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 155, 'Black & Decker India', 'https://www.blackanddecker.co.in/logo.svg', '9876543364', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 156, 'Hilti Power Tools', 'https://www.hilti.co.in/logo.png', '9876543365', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 157, 'Metabo Power Tools', 'https://www.metabo.com/images/logo.svg', '9876543366', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 158, 'Techsmith India', 'https://www.techsmith.com/images/logo.png', '9876543367', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 159, 'Fluke Networks', 'https://www.fluke.com/images/logo.svg', '9876543368', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    SELECT 160, 'Grabit Electronic Components', 'https://www.grabit.co.in/logo.png', '9876543369', '3862aea4-c59a-49f4-893a-b1ca29438f52' UNION ALL
    
    -- Automotive Accessories
    SELECT 161, 'Castrol Lubricants India', 'https://www.castrol.co.in/logo.svg', '9876543370', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 162, 'Shell Lubricants India', 'https://www.shell.co.in/images/logo.png', '9876543371', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 163, 'Mobil Oil India', 'https://www.mobil.co.in/logo.svg', '9876543372', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 164, 'Bosch Auto Accessories', 'https://www.bosch-automotive.co.in/logo.png', '9876543373', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 165, 'Exide Batteries India', 'https://www.exideindia.com/logo.svg', '9876543374', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 166, 'Amaron Batteries', 'https://www.amaronbattery.com/logo.png', '9876543375', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 167, 'Goodyear Tyres India', 'https://www.goodyear.co.in/logo.svg', '9876543376', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 168, 'Bridgestone Tyres', 'https://www.bridgestone.co.in/logo.png', '9876543377', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 169, 'Apollo Tyres India', 'https://www.apollotyres.com/logo.svg', '9876543378', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    SELECT 170, 'Michelin Tyres India', 'https://www.michelin.co.in/logo.png', '9876543379', 'ef990cb0-1782-4c75-a6f6-95dd7189ad28' UNION ALL
    
    -- Home & Kitchen
    SELECT 171, 'Prestige Appliances', 'https://www.prestigeappliances.com/logo.svg', '9876543380', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 172, 'Pigeon Kitchenware', 'https://www.pigeon.in/logo.png', '9876543381', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 173, 'Milton Thermowares', 'https://www.miltonproducts.com/logo.svg', '9876543382', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 174, 'Sumeet Cookware', 'https://www.sumeetcookware.co.in/logo.png', '9876543383', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 175, 'Bel Fourneau Cookware', 'https://www.belfourneau.com/logo.svg', '9876543384', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 176, 'Wonderchef Kitchen', 'https://www.wonderchefworld.com/logo.png', '9876543385', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 177, 'Nirlon Kitchenware', 'https://www.nirlon.com/logo.svg', '9876543386', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 178, 'Dynore Cookware', 'https://www.dynore.in/logo.png', '9876543387', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 179, 'Bergner Cookware', 'https://www.bergner.co.in/logo.svg', '9876543388', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    SELECT 180, 'Vinod Cookware', 'https://www.vinodcookware.com/logo.png', '9876543389', 'da393bb8-af72-435b-b470-326220c4198b' UNION ALL
    
    -- Health & Wellness
    SELECT 181, 'Apollo Pharmacy', 'https://www.apollopharmacy.in/logo.svg', '9876543390', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 182, 'Netmeds Online Pharmacy', 'https://www.netmeds.com/logo.png', '9876543391', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 183, 'Pharmacy Store', 'https://www.pharmastore.co.in/logo.svg', '9876543392', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 184, 'Medplus Healthcare', 'https://www.medplus.co.in/logo.png', '9876543393', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 185, 'QUA Wellness Retreat', 'https://www.quawellness.com/logo.svg', '9876543394', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 186, 'Himalaya Wellness', 'https://www.himalayawellness.org/logo.png', '9876543395', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 187, 'Yoga Alliance', 'https://www.yogaalliance.org/images/logo.svg', '9876543396', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 188, 'Fitness Guru Gym', 'https://www.fitnessguru.co.in/logo.png', '9876543397', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 189, 'MyFitnessPal Partner', 'https://www.myfitnesspal.com/images/logo.svg', '9876543398', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    SELECT 190, 'Healthkart Store', 'https://www.healthkart.com/logo.png', '9876543399', '1c5aeb0a-5a17-41c9-baac-0d7280483c06' UNION ALL
    
    -- Craft & Office Supplies
    SELECT 191, 'Archies Studio Supplies', 'https://www.archies.com/logo.svg', '9876543400', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 192, 'Paperkraft Creative Store', 'https://www.paperkraft.co.in/logo.png', '9876543401', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 193, 'Craftshala Supplies', 'https://www.craftshala.in/logo.svg', '9876543402', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 194, 'Scrapbooking India', 'https://www.scrapbookingindia.com/logo.png', '9876543403', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 195, 'DIY Craft Store', 'https://www.diycraftstore.in/logo.svg', '9876543404', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 196, 'Coloring Hub', 'https://www.coloringhub.in/logo.png', '9876543405', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 197, 'Modelling Supply Center', 'https://www.modellingcenter.co.in/logo.svg', '9876543406', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 198, 'Quilting Studio', 'https://www.quiltingstudio.co.in/logo.png', '9876543407', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 199, 'Jewelry Making Supplies', 'https://www.jewelrysupply.co.in/logo.svg', '9876543408', '828f7071-d985-434e-aac1-38671e7c85a5' UNION ALL
    SELECT 200, 'Hobby & Craft Central', 'https://www.hobbycraftcentral.in/logo.png', '9876543409', '828f7071-d985-434e-aac1-38671e7c85a5'
)
INSERT INTO seller (
    id,
    phone,
    shop_name,
    qr_code_url,
    onboarding_stage,
    category_id,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    sd.phone,
    sd.shop_name,
    sd.image_url,
    'DOCUMENT_VERIFIED'::text, -- Set as verified stage
    sd.category_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM shop_data sd;

-- Insert corresponding addresses for sellers
INSERT INTO address (
    id,
    seller_id,
    city,
    state,
    country,
    line1,
    pincode,
    latitude,
    longitude,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    s.id,
    CASE (s.id::text::bigint % 5)
        WHEN 0 THEN 'Mumbai'
        WHEN 1 THEN 'Delhi'
        WHEN 2 THEN 'Bangalore'
        WHEN 3 THEN 'Hyderabad'
        ELSE 'Chennai'
    END,
    CASE (s.id::text::bigint % 5)
        WHEN 0 THEN 'Maharashtra'
        WHEN 1 THEN 'Delhi'
        WHEN 2 THEN 'Karnataka'
        WHEN 3 THEN 'Telangana'
        ELSE 'Tamil Nadu'
    END,
    'India',
    s.shop_name || ' Business District, ' || CASE (s.id::text::bigint % 5)
        WHEN 0 THEN 'Bandra'
        WHEN 1 THEN 'Connaught Place'
        WHEN 2 THEN 'Indiranagar'
        WHEN 3 THEN 'Jubilee Hills'
        ELSE 'T Nagar'
    END,
    CASE (s.id::text::bigint % 5)
        WHEN 0 THEN '400051'
        WHEN 1 THEN '110001'
        WHEN 2 THEN '560038'
        WHEN 3 THEN '500033'
        ELSE '600017'
    END,
    CASE (s.id::text::bigint % 5)
        WHEN 0 THEN 19.0760::numeric
        WHEN 1 THEN 28.6139::numeric
        WHEN 2 THEN 12.9716::numeric
        WHEN 3 THEN 17.3850::numeric
        ELSE 13.1939::numeric
    END,
    CASE (s.id::text::bigint % 5)
        WHEN 0 THEN 72.8277::numeric
        WHEN 1 THEN 77.2090::numeric
        WHEN 2 THEN 77.5946::numeric
        WHEN 3 THEN 78.4867::numeric
        ELSE 80.2824::numeric
    END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seller s
WHERE s.phone LIKE '98765%';

-- Insert bank details for sellers
INSERT INTO seller_bank_details (
    id,
    seller_id,
    account_holder_name,
    account_number,
    ifsc_code,
    bank_name,
    account_type,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    s.id,
    s.shop_name || ' Account',
    '5' || LPAD((ROW_NUMBER() OVER ())::text, 15, '0'),
    CASE (s.id::text::bigint % 5)
        WHEN 0 THEN 'HDFC0000001'
        WHEN 1 THEN 'ICIC0000002'
        WHEN 2 THEN 'AXIS0000003'
        WHEN 3 THEN 'SBIN0000004'
        ELSE 'KOTAK000005'
    END,
    CASE (s.id::text::bigint % 5)
        WHEN 0 THEN 'HDFC Bank'
        WHEN 1 THEN 'ICICI Bank'
        WHEN 2 THEN 'Axis Bank'
        WHEN 3 THEN 'State Bank of India'
        ELSE 'Kotak Mahindra Bank'
    END,
    'Saving',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seller s
WHERE s.phone LIKE '98765%';

-- Insert OTP records for sellers (to simulate Aadhaar verification)
INSERT INTO otp (
    id,
    phone,
    otp_code,
    is_verified,
    expiry_time,
    type,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    s.phone,
    '123456',
    true,
    CURRENT_TIMESTAMP + INTERVAL '1 day',
    'aadhaarVerification'::text,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seller s
WHERE s.phone LIKE '98765%';

-- Verify data inserted
SELECT COUNT(*) as total_sellers FROM seller WHERE phone LIKE '98765%';
SELECT COUNT(*) as total_addresses FROM address WHERE id IN (SELECT id FROM address LIMIT 200);
SELECT COUNT(*) as total_bank_details FROM seller_bank_details WHERE id IN (SELECT id FROM seller_bank_details LIMIT 200);
