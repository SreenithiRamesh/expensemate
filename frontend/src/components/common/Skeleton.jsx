import { cn } from '../../utils/cn'

export function Skeleton({
                             className,
                         }) {
    return (
        <div
            aria-hidden="true"
            className={cn(
                'animate-pulse rounded-xl bg-linear-to-r from-slate-100 via-brand-50 to-slate-100 bg-[length:200%_100%]',
                className,
            )}
        />
    )
}

export function DashboardSkeleton() {
    return (
        <div
            role="status"
            aria-label="Loading dashboard"
            className="space-y-7"
        >
            <div className="space-y-3">
                <Skeleton className="h-4 w-44" />
                <Skeleton className="h-10 w-full max-w-md" />
                <Skeleton className="h-5 w-full max-w-sm" />
            </div>

            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                {[1, 2, 3].map((item) => (
                    <div
                        key={item}
                        className="rounded-3xl border border-white/80 bg-white/80 p-6"
                    >
                        <div className="flex justify-between gap-5">
                            <div className="flex-1 space-y-4">
                                <Skeleton className="h-4 w-28" />
                                <Skeleton className="h-9 w-32" />
                                <Skeleton className="h-4 w-40" />
                            </div>

                            <Skeleton className="h-12 w-12 rounded-2xl" />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}