import { Stack, Typography } from '@mui/material';
import { Check as CheckIcon, Percent as PercentIcon } from '@mui/icons-material';

interface PlanFeatureListProps {
    featuresJson: string;
    commissionRate: number;
    compact?: boolean;
}

export function PlanFeatureList({ featuresJson, commissionRate, compact = false }: PlanFeatureListProps) {
    let items: string[] = [];
    try {
        const parsed = JSON.parse(featuresJson);
        if (Array.isArray(parsed)) items = parsed;
    } catch {
        // ignore
    }

    const spacing = compact ? 0.75 : 1.25;

    return (
        <Stack spacing={spacing}>
            <Stack direction="row" alignItems="center" gap={1}>
                <PercentIcon sx={{ fontSize: 18, color: 'primary.main' }} />
                <Typography variant="body2" fontWeight="bold" color="primary.main">
                    %{commissionRate} platform komisyonu
                </Typography>
            </Stack>

            {items.map((item, i) => (
                <Stack direction="row" alignItems="flex-start" gap={1} key={i}>
                    <CheckIcon sx={{ fontSize: 16, color: 'success.main', mt: '2px', flexShrink: 0 }} />
                    <Typography variant="body2">{item}</Typography>
                </Stack>
            ))}
        </Stack>
    );
}
