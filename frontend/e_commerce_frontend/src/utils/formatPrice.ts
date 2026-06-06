export const formatPrice = (amount: number, currency = 'TRY') =>
    new Intl.NumberFormat('tr-TR', { style: 'currency', currency }).format(amount);
