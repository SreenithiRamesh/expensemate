import {
    useEffect,
    useId,
} from 'react'
import { X } from 'lucide-react'

import { cn } from '../../utils/cn'

export function Modal({
                          title,
                          description,
                          children,
                          onClose,
                          className,
                      }) {
    const titleId = useId()
    const descriptionId = useId()

    useEffect(() => {
        const previousOverflow =
            document.body.style.overflow

        document.body.style.overflow = 'hidden'

        function handleKeyDown(event) {
            if (event.key === 'Escape') {
                onClose()
            }
        }

        document.addEventListener(
            'keydown',
            handleKeyDown,
        )

        return () => {
            document.body.style.overflow =
                previousOverflow

            document.removeEventListener(
                'keydown',
                handleKeyDown,
            )
        }
    }, [onClose])

    return (
        <div className="fixed inset-0 z-[80] flex items-center justify-center p-4 sm:p-6">
            <button
                type="button"
                aria-label="Close dialog"
                onClick={onClose}
                className="absolute inset-0 bg-slate-950/45 backdrop-blur-sm"
            />

            <section
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                aria-describedby={
                    description
                        ? descriptionId
                        : undefined
                }
                className={cn(
                    'relative z-10 max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-white/80 bg-white p-6 shadow-2xl shadow-slate-950/20 sm:p-7',
                    className,
                )}
            >
                <header className="flex items-start justify-between gap-4">
                    <div>
                        <h2
                            id={titleId}
                            className="text-2xl font-black text-heading"
                        >
                            {title}
                        </h2>

                        {description && (
                            <p
                                id={descriptionId}
                                className="mt-1 text-sm leading-6 text-slate-500"
                            >
                                {description}
                            </p>
                        )}
                    </div>

                    <button
                        type="button"
                        aria-label="Close dialog"
                        onClick={onClose}
                        className="grid h-10 w-10 shrink-0 place-items-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-heading focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
                    >
                        <X
                            size={20}
                            aria-hidden="true"
                        />
                    </button>
                </header>

                <div className="mt-6">
                    {children}
                </div>
            </section>
        </div>
    )
}