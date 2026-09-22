import { Inbox } from 'lucide-react'

import { Button } from './Button'

export function EmptyState({
                               icon: Icon = Inbox,
                               title = 'Nothing here yet',
                               description = 'New information will appear here.',
                               actionLabel,
                               onAction,
                           }) {
    return (
        <div className="grid min-h-64 place-items-center rounded-3xl border border-dashed border-brand-200 bg-linear-to-br from-white/70 to-brand-50/70 px-6 py-10 text-center">
            <div className="max-w-sm">
                <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-linear-to-br from-brand-100 to-cyan-100 text-brand-700">
                    <Icon size={26} />
                </span>

                <h2 className="mt-5 text-xl font-bold text-heading">
                    {title}
                </h2>

                <p className="mt-2 leading-6 text-slate-500">
                    {description}
                </p>

                {actionLabel && onAction && (
                    <Button
                        onClick={onAction}
                        className="mt-6"
                    >
                        {actionLabel}
                    </Button>
                )}
            </div>
        </div>
    )
}