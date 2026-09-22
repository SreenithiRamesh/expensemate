import { ArrowDownCircle, ArrowUpCircle, TrendingDown, Wallet } from 'lucide-react'

import { ActivityTimeline } from '../components/dashboard/ActivityTimeline'
import { AiInsightCard } from '../components/dashboard/AiInsightCard'
import { BudgetProgress } from '../components/dashboard/BudgetProgress'
import { DashboardWelcome } from '../components/dashboard/DashboardWelcome'
import { GroupBalanceCard } from '../components/dashboard/GroupBalanceCard'
import { QuickActions } from '../components/dashboard/QuickActions'
import { RecentTransactions } from '../components/dashboard/RecentTransactions'
import { SpendingChart } from '../components/dashboard/SpendingChart'
import { SummaryCard } from '../components/dashboard/SummaryCard'

// UI-preview data only. This is the single, clearly-labelled location for
// placeholder values used by the dashboard's presentational components.
// It will be replaced with TanStack Query hooks (useDashboardSummary, etc.)
// during the M32 personal-finance milestone — components already accept
// this shape via props, so that swap won't require rewriting the UI.
const dashboardPreviewData = {
    monthlySpending: 18450,
    remainingBudget: 11550,
    owedToUser: 1250,
    userOwes: 840,
    transactions: [
        { id: 't1', title: 'Team lunch', category: 'Food', date: 'Today', amount: -640, split: true },
        { id: 't2', title: 'Metro recharge', category: 'Travel', date: 'Yesterday', amount: -300, split: false },
        { id: 't3', title: 'Freelance payment', category: 'Bills', date: '2 days ago', amount: 6000, split: false },
        { id: 't4', title: 'Grocery run', category: 'Shopping', date: '3 days ago', amount: -1450, split: true },
    ],
    activity: [
        { id: 'a1', type: 'expense', title: 'You added "Team lunch" — ₹640', timestamp: '2 hours ago' },
        { id: 'a2', type: 'member', title: 'Aisha joined "Goa Trip"', timestamp: 'Yesterday' },
        { id: 'a3', type: 'settlement', title: 'Settled ₹500 with Rahul', timestamp: '3 days ago' },
        { id: 'a4', type: 'budget', title: 'Updated "Food" budget to ₹8,000', timestamp: '5 days ago' },
    ],
}

function DashboardPage() {
    function handleQuickAction(actionKey) {
        // Presentational only — wired to real handlers once routing/forms
        // for expenses, groups, and budgets land.
        console.info('Quick action triggered:', actionKey)
    }

    return (
        <div className="space-y-6">
            <DashboardWelcome
                name="Sree"
                onAddExpense={() => handleQuickAction('add-expense')}
                onCreateGroup={() => handleQuickAction('create-group')}
            />

            <section className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
                <SummaryCard
                    title="Monthly spending"
                    value={`₹${dashboardPreviewData.monthlySpending.toLocaleString('en-IN')}`}
                    change="+8.2%"
                    trend="up"
                    icon={Wallet}
                    variant="teal"
                    sparklineData={[3, 5, 4, 6, 5, 7, 8]}
                />

                <SummaryCard
                    title="Remaining budget"
                    value={`₹${dashboardPreviewData.remainingBudget.toLocaleString('en-IN')}`}
                    change="38% left"
                    trend="neutral"
                    icon={TrendingDown}
                    variant="sky"
                    sparklineData={[8, 7, 7, 6, 6, 5, 5]}
                />

                <SummaryCard
                    title="You are owed"
                    value={`₹${dashboardPreviewData.owedToUser.toLocaleString('en-IN')}`}
                    change="2 groups"
                    trend="up"
                    icon={ArrowUpCircle}
                    variant="green"
                    sparklineData={[2, 3, 3, 4, 4, 5, 5]}
                />

                <SummaryCard
                    title="You owe"
                    value={`₹${dashboardPreviewData.userOwes.toLocaleString('en-IN')}`}
                    change="1 group"
                    trend="down"
                    icon={ArrowDownCircle}
                    variant="rose"
                    sparklineData={[6, 5, 5, 4, 3, 3, 2]}
                />
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
                <SpendingChart hasData />

                <div className="space-y-6">
                    <AiInsightCard />
                    <GroupBalanceCard
                        groupName="Goa Trip"
                        memberInitials={['S', 'A', 'R', 'K']}
                        memberCount={4}
                        balance={dashboardPreviewData.owedToUser}
                        direction="owed"
                    />
                </div>
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
                <RecentTransactions
                    transactions={dashboardPreviewData.transactions}
                    onViewAll={() => handleQuickAction('view-all-transactions')}
                />

                <BudgetProgress
                    budget={30000}
                    used={dashboardPreviewData.monthlySpending}
                />
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
                <QuickActions onAction={handleQuickAction} />
                <ActivityTimeline items={dashboardPreviewData.activity} />
            </section>
        </div>
    )
}

export default DashboardPage
