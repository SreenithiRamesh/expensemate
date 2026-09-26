import { LoaderCircle } from 'lucide-react'

export default function AuthLoadingState() {
    return (
        <main
            role="status"
            aria-live="polite"
            className="flex min-h-screen items-center justify-center px-6"
        >
            <div className="flex flex-col items-center gap-4 text-center">
                <div className="grid h-16 w-16 place-items-center rounded-3xl bg-white shadow-card">
                    <LoaderCircle
                        size={30}
                        aria-hidden="true"
                        className="animate-spin text-brand-600 motion-reduce:animate-none"
                    />
                </div>

                <div>
                    <p className="font-display text-lg font-bold text-heading">
                        Checking your session
                    </p>

                    <p className="mt-1 text-sm text-slate-500">
                        Securely preparing ExpenseMate…
                    </p>
                </div>
            </div>
        </main>
    )
}