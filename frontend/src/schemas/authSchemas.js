import { z } from 'zod'

const emailSchema = z
    .string()
    .trim()
    .min(
        1,
        'Please enter your email address.',
    )
    .max(
        150,
        'Email must be under 150 characters.',
    )
    .email(
        'Please enter a valid email address.',
    )

export const loginSchema = z.object({
    email: emailSchema,

    password: z
        .string()
        .min(
            1,
            'Please enter your password.',
        )
        .max(
            72,
            'Password must be under 72 characters.',
        ),
})

export const registerSchema = z
    .object({
        name: z
            .string()
            .trim()
            .min(
                1,
                'Please enter your name.',
            )
            .max(
                100,
                'Name must be under 100 characters.',
            ),

        email: emailSchema,

        password: z
            .string()
            .min(
                8,
                'Password must be at least 8 characters.',
            )
            .max(
                72,
                'Password must be under 72 characters.',
            ),

        confirmPassword: z
            .string()
            .min(
                1,
                'Please confirm your password.',
            ),
    })
    .refine(
        (values) =>
            values.password ===
            values.confirmPassword,
        {
            message:
                'Passwords do not match.',
            path: [
                'confirmPassword',
            ],
        },
    )