package com.streatfeast.app.models

data class MenuData(
    val categories: List<Category>,
    val items: List<MenuItem>,
    val frequentItemIds: List<String>
)


