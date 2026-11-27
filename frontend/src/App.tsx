import Background from './Landing/Background.tsx'
import Menu from './Landing/Component/Menu.tsx'
import Hero from './Landing/Hero.tsx'

export default function App() {
  return (
    <>
      <Background>
        <Hero />
      </Background>
      <Menu />
    </>
  )
}
