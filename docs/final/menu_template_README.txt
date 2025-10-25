Streat Feast — Menu Import Template (CSV)

Columns (exact headers required):
- Item Name            : String (e.g., "Veg Momos")
- Category             : String (e.g., "Chinese", "Indian"). No subcategories.
- Available Sizes      : Comma-separated values from this set only: Small, Large. Leave blank for one-size items.
- Veg/NonVeg           : Use exactly one of: Veg, NonVeg (case-insensitive accepted, will be normalized).

Examples:
Veg Momos,Chinese,Small,Large,Veg
Dal Tadka,Indian,,Veg

Tips:
- Keep one row per item.
- If you list sizes, only use Small or Large (in any order). Other values will be rejected.
- Do not include prices or images.
- Save as CSV encoded in UTF-8.