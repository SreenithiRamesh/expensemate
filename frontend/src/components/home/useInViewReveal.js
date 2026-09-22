import { useEffect, useRef, useState } from 'react'
import { useReducedMotion } from '../../hooks/useReducedMotion'

export function useInViewReveal() {
    const ref = useRef(null)
    const [hasEntered, setHasEntered] = useState(false)
    const reducedMotion = useReducedMotion()

    const supportsObserver =
        typeof IntersectionObserver !== 'undefined'

    const isVisible =
        reducedMotion || !supportsObserver || hasEntered

    useEffect(() => {
        const node = ref.current

        if (!node || isVisible) {
            return undefined
        }

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries.some((entry) => entry.isIntersecting)) {
                    setHasEntered(true)
                    observer.disconnect()
                }
            },
            {
                threshold: 0.15,
                rootMargin: '0px 0px -60px 0px',
            },
        )

        observer.observe(node)

        return () => observer.disconnect()
    }, [isVisible])

    return [ref, isVisible]
}