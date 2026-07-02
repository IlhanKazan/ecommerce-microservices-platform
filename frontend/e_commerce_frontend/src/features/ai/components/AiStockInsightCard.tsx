import React from 'react';
import { Box, Paper, Stack, Typography, Chip, Skeleton } from '@mui/material';
import { AutoAwesome, Inventory2 } from '@mui/icons-material';
import { useStockInsights } from '../../../query/useAiQueries';

interface Props {
    tenantId: number | null;
}

const suggestionChip: Record<string, { label: string; color: 'error' | 'success' | 'default' }> = {
    REORDER: { label: 'Yeniden Sipariş', color: 'error' },
    OVERSTOCK: { label: 'Fazla Stok', color: 'default' },
    OK: { label: 'Yeterli', color: 'success' },
    UNKNOWN: { label: 'Bilinmiyor', color: 'default' },
};

const AiStockInsightCard: React.FC<Props> = ({ tenantId }) => {
    const { data, isLoading, isError } = useStockInsights(tenantId);

    if (isError) return null;

    return (
        <Paper
            elevation={0}
            sx={{
                p: 2.5, borderRadius: 3, mb: 1,
                background: (t) =>
                    `linear-gradient(135deg, ${t.palette.primary.lighter ?? '#FFF3E8'} 0%, #FFFFFF 70%)`,
                border: (t) => `1px solid ${t.palette.primary.light}`,
            }}
        >
            <Stack direction="row" alignItems="center" spacing={1} mb={1.5}>
                <Box sx={{
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    width: 32, height: 32, borderRadius: '50%',
                    bgcolor: 'primary.main', color: 'primary.contrastText',
                }}>
                    <AutoAwesome fontSize="small" />
                </Box>
                <Typography variant="subtitle1" fontWeight={800}>
                    Yapay Zeka Stok Önerisi
                </Typography>
            </Stack>

            {isLoading ? (
                <>
                    <Skeleton width="100%" height={20} />
                    <Skeleton width="85%" height={20} />
                </>
            ) : (
                <>
                    <Typography variant="body2" sx={{ lineHeight: 1.7, mb: data?.signals?.length ? 1.5 : 0 }}>
                        {data?.narrative ?? 'Henüz yeterli veri yok.'}
                    </Typography>
                    {data?.signals?.slice(0, 6).map((s) => {
                        const cfg = suggestionChip[s.suggestion ?? 'UNKNOWN'] ?? suggestionChip.UNKNOWN;
                        return (
                            <Stack
                                key={s.productId}
                                direction="row"
                                alignItems="center"
                                spacing={1}
                                sx={{ py: 0.5 }}
                            >
                                <Inventory2 sx={{ fontSize: 16, color: 'text.disabled' }} />
                                <Typography variant="body2" sx={{ flex: 1, minWidth: 0 }} noWrap>
                                    {s.productName ?? `Ürün #${s.productId}`}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {s.unitsSold} satış · {s.availableQuantity ?? '—'} stok
                                </Typography>
                                <Chip size="small" label={cfg.label} color={cfg.color} variant="outlined" />
                            </Stack>
                        );
                    })}
                </>
            )}
        </Paper>
    );
};

export default AiStockInsightCard;
