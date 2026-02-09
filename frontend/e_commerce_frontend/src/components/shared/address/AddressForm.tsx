import React from 'react';
import { TextField, Grid} from '@mui/material';
import type { AddressType } from '../../../types/user';

export interface AddressFormData {
    title?: string;
    recipientName: string;
    phoneNumber: string;
    country: string;
    city: string;
    stateProvince: string;
    zipCode: string;
    line1: string;
    line2?: string;
    addressType?: AddressType;
}

interface AddressFormProps {
    data: AddressFormData;
    onChange: (field: keyof AddressFormData, value: string) => void;
    showTitle?: boolean;
}

const AddressForm: React.FC<AddressFormProps> = ({ data, onChange, showTitle = true }) => {
    return (
        <Grid container spacing={2}>
            {showTitle && (
                <Grid size={{ xs: 12 }}>
                    <TextField
                        label="Adres Başlığı (Ev, İş vb.)" fullWidth required
                        value={data.title || ''}
                        onChange={(e) => onChange('title', e.target.value)}
                        placeholder="Örn: Merkez Depo"
                    />
                </Grid>
            )}

            <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                    label="Alıcı Adı Soyadı" fullWidth required
                    value={data.recipientName}
                    onChange={(e) => onChange('recipientName', e.target.value)}
                />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                    label="Telefon Numarası" fullWidth required
                    value={data.phoneNumber}
                    onChange={(e) => onChange('phoneNumber', e.target.value)}
                />
            </Grid>

            <Grid size={{ xs: 12 }}>
                <TextField
                    label="Adres Satırı 1" fullWidth multiline rows={2} required
                    value={data.line1}
                    onChange={(e) => onChange('line1', e.target.value)}
                    placeholder="Cadde, Mahalle, Sokak, Bina No..."
                />
            </Grid>

            <Grid size={{ xs: 12 }}>
                <TextField
                    label="Adres Satırı 2 (Opsiyonel)" fullWidth
                    value={data.line2 || ''}
                    onChange={(e) => onChange('line2', e.target.value)}
                    placeholder="Daire No, Kat, Tarif..."
                />
            </Grid>

            <Grid size={{ xs: 6, md: 6 }}>
                <TextField
                    label="Şehir" fullWidth required
                    value={data.city}
                    onChange={(e) => onChange('city', e.target.value)}
                />
            </Grid>
            <Grid size={{ xs: 6, md: 6 }}>
                <TextField
                    label="İlçe / Bölge" fullWidth required
                    value={data.stateProvince}
                    onChange={(e) => onChange('stateProvince', e.target.value)}
                />
            </Grid>
            <Grid size={{ xs: 6, md: 6 }}>
                <TextField
                    label="Posta Kodu" fullWidth required
                    value={data.zipCode}
                    onChange={(e) => onChange('zipCode', e.target.value)}
                />
            </Grid>
            <Grid size={{ xs: 6, md: 6 }}>
                <TextField
                    label="Ülke" fullWidth disabled
                    value={data.country || 'Turkey'}
                />
            </Grid>
        </Grid>
    );
};

export default AddressForm;