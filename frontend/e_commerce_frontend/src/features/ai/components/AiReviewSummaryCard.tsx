import React from 'react';
import {
    Box, Card, CardContent, Chip, Stack, Typography, Skeleton, Divider,
} from '@mui/material';
import {
    AutoAwesome, SentimentSatisfiedAlt, SentimentNeutral,
    SentimentDissatisfied, ThumbUpAlt, ThumbDownAlt,
} from '@mui/icons-material';
import { useReviewSummary } from '../../../query/useAiQueries';
import type { Sentiment } from '../types';

interface Props {
    productId: number;
    /** Üründe hiç yorum yoksa hook çağrısını kısıtlamak için */
    hasReviews?: boolean;
}

const sentimentConfig: Record<Sentiment, { label: string; color: 'success' | 'warning' | 'error' | 'default'; icon: React.ReactNode }> = {
    POSITIVE: { label: 'Çoğunlukla Olumlu', color: 'success', icon: <SentimentSatisfiedAlt fontSize="small" /> },
    MIXED: { label: 'Karışık', color: 'warning', icon: <SentimentNeutral fontSize="small" /> },
    NEUTRAL: { label: 'Nötr', color: 'default', icon: <SentimentNeutral fontSize="small" /> },
    NEGATIVE: { label: 'Çoğunlukla Olumsuz', color: 'error', icon: <SentimentDissatisfied fontSize="small" /> },
};

const AiReviewSummaryCard: React.FC<Props> = ({ productId, hasReviews = true }) => {
    const { data, isLoading, isError } = useReviewSummary(productId, hasReviews);

    if (!hasReviews) return null;

    if (isLoading) {
        return (
            <Card variant="outlined" sx={{ mb: 3, borderRadius: 3, borderColor: 'primary.light' }}>
                <CardContent>
                    <Skeleton width={180} height={28} />
                    <Skeleton width="100%" height={20} sx={{ mt: 1 }} />
                    <Skeleton width="80%" height={20} />
                </CardContent>
            </Card>
        );
    }

    // Hata, özet yok veya yorum yoksa kartı gösterme
    if (isError || !data || !data.summary || data.reviewCount === 0) return null;

    const sentiment = data.overallSentiment ? sentimentConfig[data.overallSentiment] : null;

    return (
        <Card
            elevation={0}
            sx={{
                mb: 3,
                borderRadius: 3,
                background: (t) =>
                    `linear-gradient(135deg, ${t.palette.primary.lighter ?? '#FFF3E8'} 0%, #FFFFFF 70%)`,
                border: (t) => `1px solid ${t.palette.primary.light}`,
            }}
        >
            <CardContent>
                <Stack direction="row" alignItems="center" spacing={1} mb={1.5}>
                    <Box
                        sx={{
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            width: 32, height: 32, borderRadius: '50%',
                            bgcolor: 'primary.main', color: 'primary.contrastText',
                        }}
                    >
                        <AutoAwesome fontSize="small" />
                    </Box>
                    <Typography variant="subtitle1" fontWeight={800}>
                        Yapay Zeka Yorum Özeti
                    </Typography>
                    <Chip
                        size="small"
                        label={`${data.reviewCount} yorum`}
                        sx={{ ml: 'auto', fontWeight: 600 }}
                    />
                </Stack>

                {sentiment && (
                    <Chip
                        size="small"
                        icon={sentiment.icon as React.ReactElement}
                        label={sentiment.label}
                        color={sentiment.color}
                        variant="outlined"
                        sx={{ mb: 1.5, fontWeight: 600 }}
                    />
                )}

                <Typography variant="body2" color="text.primary" sx={{ lineHeight: 1.7 }}>
                    {data.summary}
                </Typography>

                {(data.pros.length > 0 || data.cons.length > 0) && (
                    <>
                        <Divider sx={{ my: 1.5 }} />
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                            {data.pros.length > 0 && (
                                <Box flex={1}>
                                    <Stack direction="row" spacing={0.5} alignItems="center" mb={0.5}>
                                        <ThumbUpAlt fontSize="small" color="success" />
                                        <Typography variant="caption" fontWeight={700} color="success.main">
                                            Beğenilenler
                                        </Typography>
                                    </Stack>
                                    <Stack component="ul" sx={{ pl: 2, m: 0 }} spacing={0.25}>
                                        {data.pros.map((p, i) => (
                                            <Typography key={i} component="li" variant="caption" color="text.secondary">
                                                {p}
                                            </Typography>
                                        ))}
                                    </Stack>
                                </Box>
                            )}
                            {data.cons.length > 0 && (
                                <Box flex={1}>
                                    <Stack direction="row" spacing={0.5} alignItems="center" mb={0.5}>
                                        <ThumbDownAlt fontSize="small" color="error" />
                                        <Typography variant="caption" fontWeight={700} color="error.main">
                                            Eleştirilenler
                                        </Typography>
                                    </Stack>
                                    <Stack component="ul" sx={{ pl: 2, m: 0 }} spacing={0.25}>
                                        {data.cons.map((c, i) => (
                                            <Typography key={i} component="li" variant="caption" color="text.secondary">
                                                {c}
                                            </Typography>
                                        ))}
                                    </Stack>
                                </Box>
                            )}
                        </Stack>
                    </>
                )}

                {data.keywords.length > 0 && (
                    <Stack direction="row" flexWrap="wrap" gap={0.5} sx={{ mt: 1.5 }}>
                        {data.keywords.map((k) => (
                            <Chip key={k} size="small" label={`#${k}`} variant="outlined" sx={{ height: 22 }} />
                        ))}
                    </Stack>
                )}

                <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 1.5 }}>
                    Bu özet, müşteri yorumlarından yapay zeka ile otomatik oluşturulmuştur.
                </Typography>
            </CardContent>
        </Card>
    );
};

export default AiReviewSummaryCard;
