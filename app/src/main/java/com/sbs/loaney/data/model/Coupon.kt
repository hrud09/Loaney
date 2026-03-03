package com.sbs.loaney.data.model

import androidx.compose.ui.graphics.Color

enum class CouponCategory {
    FOOD, TRANSPORT, FASHION, TECH, GROCERY
}

data class Coupon(
    val id: String,
    val brandName: String,
    val discountTitle: String,
    val description: String,
    val costInPies: Int,
    val category: CouponCategory,
    val brandColor: Color
)

val mockCoupons = listOf(
    Coupon(
        id = "fp_1",
        brandName = "Foodpanda",
        discountTitle = "৳150 Off",
        description = "On your next meal order above ৳500",
        costInPies = 500,
        category = CouponCategory.FOOD,
        brandColor = Color(0xFFE2136E)
    ),
    Coupon(
        id = "uber_1",
        brandName = "Uber",
        discountTitle = "50% Off Ride",
        description = "Maximum discount ৳100 on UberX",
        costInPies = 300,
        category = CouponCategory.TRANSPORT,
        brandColor = Color(0xFF000000)
    ),
    Coupon(
        id = "bata_1",
        brandName = "BATA",
        discountTitle = "৳500 Voucher",
        description = "Redeemable on all footwear at BATA outlets",
        costInPies = 1500,
        category = CouponCategory.FASHION,
        brandColor = Color(0xFFD81E05)
    ),
    Coupon(
        id = "apex_1",
        brandName = "APEX",
        discountTitle = "15% Discount",
        description = "On minimum purchase of ৳2000",
        costInPies = 800,
        category = CouponCategory.FASHION,
        brandColor = Color(0xFF00468C)
    ),
    Coupon(
        id = "star_1",
        brandName = "StarTech",
        discountTitle = "৳1000 Discount",
        description = "On selected laptops and PC components",
        costInPies = 3000,
        category = CouponCategory.TECH,
        brandColor = Color(0xFFEF4023)
    ),
    Coupon(
        id = "ryans_1",
        brandName = "Ryans",
        discountTitle = "5% Cashback",
        description = "Up to ৳500 on tech accessories",
        costInPies = 1200,
        category = CouponCategory.TECH,
        brandColor = Color(0xFF007DC4)
    ),
    Coupon(
        id = "swapno_1",
        brandName = "Shwapno",
        discountTitle = "৳200 Off",
        description = "On your monthly grocery bill over ৳3000",
        costInPies = 700,
        category = CouponCategory.GROCERY,
        brandColor = Color(0xFF00A651)
    )
)
