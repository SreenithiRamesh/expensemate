import HomeHeader from '../components/home/HomeHeader.jsx'
import HeroSection from '../components/home/HeroSection.jsx'
import FeatureSection from '../components/home/FeatureSection.jsx'
import HowItWorksSection from '../components/home/HowItWorksSection.jsx'
import ProductPreview from '../components/home/ProductPreview.jsx'
import FinalCtaSection from '../components/home/FinalCtaSection.jsx'
import HomeFooter from '../components/home/HomeFooter.jsx'

function HomePage() {
    return (
        <div className="relative isolate min-h-screen overflow-x-hidden">
            <HomeHeader />
            <main>
                <HeroSection />
                <FeatureSection />
                <HowItWorksSection />
                <ProductPreview />
                <FinalCtaSection />
            </main>
            <HomeFooter />
        </div>
    )
}

export default HomePage