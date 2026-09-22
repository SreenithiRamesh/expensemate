import { cn } from '../../utils/cn'

export function Card({
                         children,
                         className,
                         interactive = false,
                         ...props
                     }) {
    return (
        <section
            className={cn(
                'rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl',
                interactive &&
                'transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-brand-800/10',
                className,
            )}
            {...props}
        >
            {children}
        </section>
    )
}

export function CardHeader({
                               children,
                               className,
                           }) {
    return (
        <div
            className={cn(
                'mb-5 flex items-start justify-between gap-4',
                className,
            )}
        >
            {children}
        </div>
    )
}

export function CardTitle({
                              children,
                              className,
                          }) {
    return (
        <h2
            className={cn(
                'text-xl font-bold text-heading',
                className,
            )}
        >
            {children}
        </h2>
    )
}

export function CardDescription({
                                    children,
                                    className,
                                }) {
    return (
        <p
            className={cn(
                'mt-1 text-sm leading-6 text-slate-500',
                className,
            )}
        >
            {children}
        </p>
    )
}

export function CardContent({
                                children,
                                className,
                            }) {
    return (
        <div className={className}>
            {children}
        </div>
    )
}