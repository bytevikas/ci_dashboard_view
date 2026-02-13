/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: '#F26522',       // Cars24 orange
        dark: '#192B5D',          // Cars24 / CarInfo navy
        accent: '#E84C3D',        // warm red for destructive actions
        teal: '#00B8C2',          // CarInfo teal accent
        appBg: '#FFF9F5',         // warm off-white
        pastelBlue: '#E0F7F7',    // light teal tint
        pastelGreen: '#DCFCE7',
        pastelOrange: '#FFEDD5',
        pastelBrand: '#FFF0E6',   // light orange tint (brand pastel)
        softPeach: '#FFF4ED',
      },
      fontFamily: {
        sans: ['Quicksand', 'sans-serif'],
      },
      borderRadius: {
        '2xl': '1.5rem',
        '3xl': '2rem',
      },
    },
  },
  plugins: [],
}
