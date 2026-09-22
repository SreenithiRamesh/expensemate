import {
    render,
    screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
    describe,
    expect,
    it,
} from 'vitest'

import { Input } from './Input'
import { Select } from './Select'
import { Textarea } from './Textarea'

describe('form controls', () => {
    it('associates the input label with its control', async () => {
        const user =
            userEvent.setup()

        render(
            <Input
                label="Email address"
                placeholder="name@example.com"
            />,
        )

        const input =
            screen.getByLabelText(
                /email address/i,
            )

        await user.type(
            input,
            'sree@example.com',
        )

        expect(input).toHaveValue(
            'sree@example.com',
        )
    })

    it('exposes an input validation error', () => {
        render(
            <Input
                label="Amount"
                error="Amount must be greater than zero"
            />,
        )

        const input =
            screen.getByLabelText(/amount/i)

        expect(input).toHaveAttribute(
            'aria-invalid',
            'true',
        )

        expect(
            screen.getByRole('alert'),
        ).toHaveTextContent(
            'Amount must be greater than zero',
        )
    })

    it('allows a select option to be chosen', async () => {
        const user =
            userEvent.setup()

        render(
            <Select label="Category">
                <option value="">
                    Select category
                </option>

                <option value="FOOD">
                    Food
                </option>

                <option value="TRAVEL">
                    Travel
                </option>
            </Select>,
        )

        const select =
            screen.getByLabelText(
                /category/i,
            )

        await user.selectOptions(
            select,
            'TRAVEL',
        )

        expect(select).toHaveValue(
            'TRAVEL',
        )
    })

    it('allows text to be entered in a textarea', async () => {
        const user =
            userEvent.setup()

        render(
            <Textarea label="Description" />,
        )

        const textarea =
            screen.getByLabelText(
                /description/i,
            )

        await user.type(
            textarea,
            'Team lunch expense',
        )

        expect(textarea).toHaveValue(
            'Team lunch expense',
        )
    })
})