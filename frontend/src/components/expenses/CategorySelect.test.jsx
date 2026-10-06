import {
    fireEvent,
    render,
    screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { CategorySelect } from './CategorySelect'

describe('CategorySelect', () => {
    it('renders the category placeholder', () => {
        render(
            <CategorySelect
                value=""
                onChange={vi.fn()}
            />,
        )

        expect(
            screen.getByText('Select category'),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        ).toHaveAttribute(
            'aria-expanded',
            'false',
        )
    })

    it('shows all styled category options', async () => {
        const user = userEvent.setup()

        render(
            <CategorySelect
                value=""
                onChange={vi.fn()}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        )

        expect(
            screen.getByRole('listbox', {
                name: 'Category',
            }),
        ).toBeInTheDocument()

        const expectedCategories = [
            'Food & dining',
            'Travel',
            'Shopping',
            'Bills',
            'Entertainment',
            'Health',
            'Education',
            'Rent',
            'Subscription',
            'Other',
        ]

        expectedCategories.forEach(
            (category) => {
                expect(
                    screen.getByRole('option', {
                        name: category,
                    }),
                ).toBeInTheDocument()
            },
        )
    })

    it('selects a category with the mouse', async () => {
        const user = userEvent.setup()
        const onChange = vi.fn()
        const onBlur = vi.fn()

        render(
            <CategorySelect
                value=""
                onChange={onChange}
                onBlur={onBlur}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        )

        await user.click(
            screen.getByRole('option', {
                name: 'Subscription',
            }),
        )

        expect(onChange).toHaveBeenCalledWith(
            'SUBSCRIPTION',
        )

        expect(onBlur).toHaveBeenCalledTimes(1)

        expect(
            screen.queryByRole('listbox'),
        ).not.toBeInTheDocument()
    })

    it('supports keyboard selection', async () => {
        const user = userEvent.setup()
        const onChange = vi.fn()

        render(
            <CategorySelect
                value=""
                onChange={onChange}
            />,
        )

        const trigger =
            screen.getByRole('button', {
                name: /category select category/i,
            })

        trigger.focus()

        await user.keyboard(
            '{ArrowDown}{ArrowDown}{Enter}',
        )

        expect(onChange).toHaveBeenCalledWith(
            'TRAVEL',
        )

        expect(
            screen.queryByRole('listbox'),
        ).not.toBeInTheDocument()
    })

    it('renders the all-categories option for filters', async () => {
        const user = userEvent.setup()
        const onChange = vi.fn()

        render(
            <CategorySelect
                includeAll
                value="FOOD"
                onChange={onChange}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: /category food & dining/i,
            }),
        )

        expect(
            screen.getByRole('option', {
                name: 'Food & dining',
            }),
        ).toHaveAttribute(
            'aria-selected',
            'true',
        )

        await user.click(
            screen.getByRole('option', {
                name: 'All categories',
            }),
        )

        expect(onChange).toHaveBeenCalledWith('')
    })

    it('closes when Escape is pressed', async () => {
        const user = userEvent.setup()

        render(
            <CategorySelect
                value=""
                onChange={vi.fn()}
            />,
        )

        const trigger =
            screen.getByRole('button', {
                name: /category select category/i,
            })

        await user.click(trigger)

        expect(
            screen.getByRole('listbox'),
        ).toBeInTheDocument()

        await user.keyboard('{Escape}')

        expect(
            screen.queryByRole('listbox'),
        ).not.toBeInTheDocument()
    })

    it('closes when the user clicks outside', async () => {
        const user = userEvent.setup()

        render(
            <div>
                <CategorySelect
                    value=""
                    onChange={vi.fn()}
                />

                <button type="button">
                    Outside
                </button>
            </div>,
        )

        await user.click(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        )

        expect(
            screen.getByRole('listbox'),
        ).toBeInTheDocument()

        fireEvent.pointerDown(
            screen.getByRole('button', {
                name: 'Outside',
            }),
        )

        expect(
            screen.queryByRole('listbox'),
        ).not.toBeInTheDocument()
    })

    it('displays validation errors', () => {
        render(
            <CategorySelect
                value=""
                onChange={vi.fn()}
                required
                error="Please select a category."
            />,
        )

        expect(
            screen.getByRole('alert'),
        ).toHaveTextContent(
            'Please select a category.',
        )

        expect(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        ).toHaveAttribute(
            'aria-invalid',
            'true',
        )
    })

    it('does not open when disabled', async () => {
        const user = userEvent.setup()

        render(
            <CategorySelect
                value=""
                onChange={vi.fn()}
                disabled
            />,
        )

        const trigger =
            screen.getByRole('button', {
                name: /category select category/i,
            })

        expect(trigger).toBeDisabled()

        await user.click(trigger)

        expect(
            screen.queryByRole('listbox'),
        ).not.toBeInTheDocument()
    })
})