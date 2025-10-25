/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        'status-created': '#3B82F6',      // blue
        'status-accepted': '#F97316',     // orange
        'status-inkitchen': '#EAB308',    // yellow
        'status-prepared': '#22C55E',     // green
        'status-delivered': '#A855F7',    // purple
        'status-closed': '#6B7280',       // gray
        'status-canceled': '#EF4444',     // red
        'action-primary': '#22C55E',      // green
        'action-danger': '#EF4444',       // red
      }
    }
  },
  plugins: [],
}

