import { Category, Item } from '../store/menuStore';

export const seedMenu = () => {
  const now = Date.now();
  
  const categories: Category[] = [
    {
      id: 'seed-cat-1',
      name: 'Chinese',
      isActive: true,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'seed-cat-2',
      name: 'Indian',
      isActive: true,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'seed-cat-3',
      name: 'Desserts',
      isActive: true,
      createdAt: now,
      updatedAt: now
    }
  ];

  const items: Item[] = [
    // Chinese
    {
      id: 'seed-item-1',
      categoryId: 'seed-cat-1',
      name: 'Chicken Soup',
      sizes: ['Small', 'Large'],
      vegFlag: 'NonVeg',
      isActive: true,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'seed-item-2',
      categoryId: 'seed-cat-1',
      name: 'Spring Rolls',
      sizes: [],
      vegFlag: 'Veg',
      isActive: true,
      createdAt: now,
      updatedAt: now
    },
    // Indian
    {
      id: 'seed-item-3',
      categoryId: 'seed-cat-2',
      name: 'Paneer Tikka',
      sizes: ['Small', 'Large'],
      vegFlag: 'Veg',
      isActive: true,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'seed-item-4',
      categoryId: 'seed-cat-2',
      name: 'Butter Chicken',
      sizes: ['Small', 'Large'],
      vegFlag: 'NonVeg',
      isActive: true,
      createdAt: now,
      updatedAt: now
    },
    // Desserts
    {
      id: 'seed-item-5',
      categoryId: 'seed-cat-3',
      name: 'Chocolate Cake',
      sizes: [],
      vegFlag: 'Veg',
      isActive: true,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'seed-item-6',
      categoryId: 'seed-cat-3',
      name: 'Ice Cream',
      sizes: ['Small', 'Large'],
      vegFlag: 'Veg',
      isActive: true,
      createdAt: now,
      updatedAt: now
    }
  ];

  return { categories, items };
};

