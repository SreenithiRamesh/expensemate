import {
    ArrowDownCircle,
    ArrowUpCircle,
    TrendingDown,
    Wallet,
} from 'lucide-react'
import { useOutletContext } from 'react-router-dom'

import { ActivityTimeline } from '../components/dashboard/ActivityTimeline'
import { AiInsightCard } from '../components/dashboard/AiInsightCard'
import { BudgetProgress } from '../components/dashboard/BudgetProgress'
import { DashboardWelcome } from '../components/dashboard/DashboardWelcome'
import { GroupBalanceCard } from '../components/dashboard/GroupBalanceCard'
import { QuickActions } from '../components/dashboard/QuickActions'
import { RecentTransactions } from '../components/dashboard/RecentTransactions'
import { SpendingChart } from '../components/dashboard/SpendingChart'
import { SummaryCard } from '../components/dashboard/SummaryCard'

/*
 * UI-preview data only.
 *
 * Replace this object with real TanStack Query API data during M32.
 * Weekly data represents Week 4 of this illustrative month.
 */
const dashboardPreviewData = {
    monthlyBudget: 30000,
    owedToUser: 1250,
    userOwes: 840,

    spendingSeries: {
        weekly: {
            caption: 'Week 4 · Daily spending',

            points: [
                {
                    label: 'Mon',
                    amount: 500,
                },
                {
                    label: 'Tue',
                    amount: 640,
                },
                {
                    label: 'Wed',
                    amount: 300,
                },
                {
                    label: 'Thu',
                    amount: 1450,
                },
                {
                    label: 'Fri',
                    amount: 400,
                },
                {
                    label: 'Sat',
                    amount: 600,
                },
                {
                    label: 'Sun',
                    amount: 500,
                },
            ],

            categories: [
                {
                    label: 'Food & dining',
                    value: 1240,
                    color: '#159FA4',
                },
                {
                    label: 'Travel',
                    value: 700,
                    color: '#3E8EF7',
                },
                {
                    label: 'Shopping',
                    value: 1450,
                    color: '#E9B949',
                },
                {
                    label: 'Bills',
                    value: 1000,
                    color: '#2F9E6F',
                },
            ],
        },

        monthly: {
            caption: 'This month · Weekly spending',

            points: [
                {
                    label: 'Wk 1',
                    amount: 4230,
                },
                {
                    label: 'Wk 2',
                    amount: 5110,
                },
                {
                    label: 'Wk 3',
                    amount: 4720,
                },
                {
                    label: 'Wk 4',
                    amount: 4390,
                },
            ],

            categories: [
                {
                    label: 'Food & dining',
                    value: 6240,
                    color: '#159FA4',
                },
                {
                    label: 'Travel',
                    value: 4120,
                    color: '#3E8EF7',
                },
                {
                    label: 'Shopping',
                    value: 3080,
                    color: '#E9B949',
                },
                {
                    label: 'Bills',
                    value: 5010,
                    color: '#2F9E6F',
                },
            ],
        },
    },

    budgetCategories: [
        {
            label: 'Food',
            used: 6240,
            budget: 8000,
        },
        {
            label: 'Travel',
            used: 4120,
            budget: 5000,
        },
        {
            label: 'Shopping',
            used: 3080,
            budget: 4500,
        },
        {
            label: 'Bills',
            used: 5010,
            budget: 6000,
        },
    ],

    transactions: [
        {
            id: 't1',
            title: 'Team lunch',
            category: 'Food',
            date: 'Today',
            amount: -640,
            split: true,
        },
        {
            id: 't2',
            title: 'Metro recharge',
            category: 'Travel',
            date: 'Yesterday',
            amount: -300,
            split: false,
        },
        {
            id: 't3',
            title: 'Internet bill',
            category: 'Bills',
            date: '2 days ago',
            amount: -1000,
            split: false,
        },
        {
            id: 't4',
            title: 'Grocery run',
            category: 'Shopping',
            date: '3 days ago',
            amount: -1450,
            split: true,
        },
    ],

    activity: [
        {
            id: 'a1',
            type: 'expense',
            title: 'You added "Team lunch" — ₹640',
            timestamp: '2 hours ago',
        },
        {
            id: 'a2',
            type: 'member',
            title: 'Aisha joined "Goa Trip"',
            timestamp: 'Yesterday',
        },
        {
            id: 'a3',
            type: 'settlement',
            title: 'Settled ₹500 with Rahul',
            timestamp: '3 days ago',
        },
        {
            id: 'a4',
            type: 'budget',
            title: 'Updated "Food" budget to ₹8,000',
            timestamp: '5 days ago',
        },
    ],
}

function formatCurrency(value) {
    return `₹${value.toLocaleString('en-IN')}`
}

function DashboardPage() {
    /*
     * DashboardShell passes the authenticated session through
     * React Router's Outlet context.
     */
    const outletContext = useOutletContext()
    const user = outletContext?.user ?? null

    const displayName =
        user?.name?.trim() ||
        'there'
    const monthlyPoints =
        dashboardPreviewData.spendingSeries.monthly.points

    const monthlySpending =
        monthlyPoints.reduce(
            (total, point) =>
                total + point.amount,
            0,
        )

    const remainingBudget =
        dashboardPreviewData.monthlyBudget -
        monthlySpending

    const remainingPercent =
        Math.round(
            (
                Math.max(
                    remainingBudget,
                    0,
                ) /
                dashboardPreviewData.monthlyBudget
            ) * 100,
        )

    const monthlySpendingSparkline =
        monthlyPoints.map(
            (point) => point.amount,
        )

    const remainingBudgetSparkline =
        monthlyPoints.map(
            (_, index) => {
                const spendingUntilCurrentWeek =
                    monthlyPoints
                        .slice(
                            0,
                            index + 1,
                        )
                        .reduce(
                            (total, point) =>
                                total + point.amount,
                            0,
                        )

                return Math.max(
                    dashboardPreviewData.monthlyBudget -
                    spendingUntilCurrentWeek,
                    0,
                )
            },
        )

    function handleQuickAction(actionKey) {
        /*
         * These actions remain presentational during M31.
         *
         * They will navigate to their corresponding forms and
         * routes when the relevant feature milestone is built.
         */
        console.info(
            'Quick action triggered:',
            actionKey,
        )
    }

    return (
        <div className="space-y-6">
            <DashboardWelcome
                name={displayName}
                onAddExpense={() =>
                    handleQuickAction(
                        'add-expense',
                    )
                }
                onCreateGroup={() =>
                    handleQuickAction(
                        'create-group',
                    )
                }
            />

            <p className="text-xs font-semibold text-slate-500">
                Dashboard preview — amounts, activity, and insights are
                illustrative sample data.
            </p>

            <section
                aria-label="Financial summary"
                className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4"
            >
                <SummaryCard
                    title="Monthly spending"
                    value={formatCurrency(
                        monthlySpending,
                    )}
                    change="This month"
                    trend="neutral"
                    icon={Wallet}
                    variant="teal"
                    sparklineData={
                        monthlySpendingSparkline
                    }
                />

                <SummaryCard
                    title="Remaining budget"
                    value={formatCurrency(
                        remainingBudget,
                    )}
                    change={`${remainingPercent}% left`}
                    trend="neutral"
                    icon={TrendingDown}
                    variant="sky"
                    sparklineData={
                        remainingBudgetSparkline
                    }
                />

                <SummaryCard
                    title="You are owed"
                    value={formatCurrency(
                        dashboardPreviewData.owedToUser,
                    )}
                    change="2 groups"
                    trend="up"
                    icon={ArrowUpCircle}
                    variant="green"
                    sparklineData={[
                        2,
                        3,
                        3,
                        4,
                        4,
                        5,
                        5,
                    ]}
                />

                <SummaryCard
                    title="You owe"
                    value={formatCurrency(
                        dashboardPreviewData.userOwes,
                    )}
                    change="1 group"
                    trend="down"
                    icon={ArrowDownCircle}
                    variant="rose"
                    sparklineData={[
                        6,
                        5,
                        5,
                        4,
                        3,
                        3,
                        2,
                    ]}
                />
            </section>

            <section
                aria-label="Spending and group overview"
                className="grid gap-6 xl:grid-cols-[1.5fr_1fr]"
            >
                <SpendingChart
                    series={
                        dashboardPreviewData.spendingSeries
                    }
                    monthlyBudget={
                        dashboardPreviewData.monthlyBudget
                    }
                    monthlySpending={
                        monthlySpending
                    }
                    isPreview
                />

                <div className="space-y-6">
                    <AiInsightCard />

                    <GroupBalanceCard
                        groupName="Goa Trip"
                        memberInitials={[
                            'S',
                            'A',
                            'R',
                            'K',
                        ]}
                        memberCount={4}
                        balance={
                            dashboardPreviewData.owedToUser
                        }
                        direction="owed"
                    />
                </div>
            </section>

            <section
                aria-label="Transactions and budget"
                className="grid gap-6 xl:grid-cols-[1.5fr_1fr]"
            >
                <RecentTransactions
                    transactions={
                        dashboardPreviewData.transactions
                    }
                    onViewAll={() =>
                        handleQuickAction(
                            'view-all-transactions',
                        )
                    }
                />

                <BudgetProgress
                    budget={
                        dashboardPreviewData.monthlyBudget
                    }
                    used={monthlySpending}
                    categories={
                        dashboardPreviewData.budgetCategories
                    }
                />
            </section>

            <section
                aria-label="Quick actions and recent activity"
                className="grid gap-6 xl:grid-cols-[1.5fr_1fr]"
            >
                <QuickActions
                    onAction={
                        handleQuickAction
                    }
                />

                <ActivityTimeline
                    items={
                        dashboardPreviewData.activity
                    }
                />
            </section>
        </div>
    )
}

export default DashboardPage