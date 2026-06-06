export const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString('tr-TR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
    });

export const formatDateTime = (iso: string) =>
    new Date(iso).toLocaleString('tr-TR');
