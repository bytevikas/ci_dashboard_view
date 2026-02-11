/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: '#363C6D',
        appBg: '#FCF9F6',
        accent: '#FF8C94',
        pastelBlue: '#E0F2FE',
        pastelGreen: '#DCFCE7',
        pastelOrange: '#FFEDD5',
        pastelPurple: '#F3E8FF',
        softPeach: '#FFF1F2',
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
