import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

export default function Layout() {
  return (
    <div className="min-h-screen flex">
      <Sidebar />
      <main className="md:ml-20 min-h-screen pb-20 flex-1">
        <Header />
        <Outlet />
      </main>
    </div>
  )
}
