package com.moataz.motazalqmi.data.local

object SeedData {
    val officialCategories = listOf(
        CategoryEntity("general", "general", "كتابات عامة", "مقالات فكرية وثقافية متنوعة", 1, 0),
        CategoryEntity("quotes", "quotes", "اقتباسات", "مختارات أدبية وفكرية موجزة", 2, 0),
        CategoryEntity("books", "books", "من بطون الكتب", "قراءات ومقتطفات من التراث والكتب", 3, 0),
        CategoryEntity("pens", "pens", "من وحي الأقلام", "خواطر ونصوص إبداعية", 4, 0),
        CategoryEntity("hadith-news", "hadith-news", "أحاديث وأخبار", "مرويات وأخبار ثقافية وتاريخية", 5, 0),
        CategoryEntity("wisdom", "wisdom", "حكم ومواعظ", "حكم بليغة ومواعظ إنسانية", 6, 0),
        CategoryEntity("arabic-literature", "arabic-literature", "أدبنا العربي", "الشعر العربي الكلاسيكي والحديث ونقده", 7, 0),
        CategoryEntity("about", "about", "حول المدونة", "التعريف بمدونة معتز العلقمي ورسالتها", 8, 0)
    )
}
