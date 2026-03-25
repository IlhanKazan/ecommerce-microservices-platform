import React, { useEffect, useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, TextField, MenuItem, Grid, InputAdornment, FormControlLabel, Checkbox,
    Typography, IconButton, Box, Divider
} from '@mui/material';
import {
    Person as PersonIcon,
    Phone as PhoneIcon,
    Home as HomeIcon,
    LocationCity as CityIcon,
    Public as GlobeIcon,
    MarkunreadMailbox as ZipIcon,
    Label as LabelIcon,
    Close as CloseIcon,
    Business as BusinessIcon
} from '@mui/icons-material';
import type { CreateAddressRequest, Address } from '../../types/user';
import { AddressType } from '../../types/enums';

interface AddressFormModalProps {
    open: boolean;
    onClose: () => void;
    onSubmit: (data: CreateAddressRequest) => Promise<void>;
    initialData?: Address | null;
    isSubmitting: boolean;
    hasOtherAddresses: boolean;
}

const emptyState: CreateAddressRequest = {
    label: '', recipientName: '', phoneNumber: '', country: 'TR',
    city: '', stateProvince: '', zipCode: '', line1: '', line2: '',
    addressType: AddressType.SHIPPING, isDefault: false
};

const AddressFormModal: React.FC<AddressFormModalProps> = ({
                                                               open, onClose, onSubmit, initialData, isSubmitting, hasOtherAddresses
                                                           }) => {
    const [formData, setFormData] = useState<CreateAddressRequest>(emptyState);

    useEffect(() => {
        if (open) {
            if (initialData) {
                setFormData({
                    label: initialData.label ?? '',
                    recipientName: initialData.recipientName ?? '',
                    phoneNumber: initialData.phoneNumber ?? '',
                    country: initialData.country ?? 'TR',
                    city: initialData.city ?? '',
                    stateProvince: initialData.stateProvince ?? '',
                    zipCode: initialData.zipCode ?? '',
                    line1: initialData.line1 ?? '',
                    line2: initialData.line2 ?? '',
                    addressType: initialData.addressType,
                    isDefault: initialData.isDefault || false
                });
            } else {
                setFormData(emptyState);
            }
        }
    }, [open, initialData]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, checked, type } = e.target;
        setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    };

    const handleSubmit = () => {
        onSubmit(formData);
    };

    const SectionHeader = ({ title }: { title: string }) => (
        <Box sx={{ mt: 2, mb: 1.5 }}>
            <Typography variant="subtitle2" color="text.secondary" fontWeight="700" textTransform="uppercase" fontSize="0.75rem" letterSpacing={1}>
                {title}
            </Typography>
            <Divider sx={{ mt: 0.5 }} />
        </Box>
    );

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="md"
            PaperProps={{
                sx: {
                    borderRadius: 4,
                    backgroundImage: 'none',
                    boxShadow: '0px 10px 40px rgba(0,0,0,0.1)'
                }
            }}
        >
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 3, pb: 1 }}>
                <Typography fontWeight="800">
                    {initialData ? 'Adresi Düzenle' : 'Yeni Adres Ekle'}
                </Typography>
                <IconButton onClick={onClose} size="small" sx={{ bgcolor: 'action.hover' }}>
                    <CloseIcon fontSize="small" />
                </IconButton>
            </DialogTitle>

            <DialogContent sx={{ p: 3, pt: 0 }}>
                <SectionHeader title="Adres Tanımı" />
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, md: 6 }}>
                        <TextField
                            label="Adres Başlığı (Ev, İş)"
                            name="label" required fullWidth
                            value={formData.label} onChange={handleChange}
                            placeholder="Örn: Evim"
                            autoComplete="off"
                            slotProps={{ htmlInput: { maxLength: 50 } }}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"><LabelIcon color="action" fontSize="small" /></InputAdornment>,
                                sx: { borderRadius: 3 }
                            }}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, md: 6 }}>
                        <TextField select label="Adres Tipi" name="addressType" fullWidth value={formData.addressType} onChange={handleChange}
                                   InputProps={{ sx: { borderRadius: 3 } }}>
                            <MenuItem value={AddressType.SHIPPING}>Teslimat Adresi</MenuItem>
                            <MenuItem value={AddressType.BILLING}>Fatura Adresi</MenuItem>
                        </TextField>
                    </Grid>
                </Grid>

                <SectionHeader title="İletişim Bilgileri" />
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, md: 6 }}>
                        <TextField
                            label="Alıcı Adı Soyadı"
                            name="recipientName" required fullWidth
                            value={formData.recipientName} onChange={handleChange}
                            autoComplete="off"
                            spellCheck={false}
                            slotProps={{ htmlInput: { maxLength: 100 } }}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"><PersonIcon color="action" fontSize="small" /></InputAdornment>,
                                sx: { borderRadius: 3 }
                            }}
                        />
                    </Grid>

                    <Grid size={{ xs: 12, md: 6 }}>
                        <TextField
                            label="Telefon Numarası"
                            name="phoneNumber" required fullWidth
                            value={formData.phoneNumber} onChange={handleChange}
                            placeholder="05XX..."
                            autoComplete="off"
                            slotProps={{ htmlInput: { maxLength: 15 } }}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"><PhoneIcon color="action" fontSize="small" /></InputAdornment>,
                                sx: { borderRadius: 3 }
                            }}
                        />
                    </Grid>
                </Grid>

                <SectionHeader title="Lokasyon ve Detay" />
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, md: 4 }}>
                        <TextField
                            label="Şehir (İl)"
                            name="city" required fullWidth
                            value={formData.city} onChange={handleChange}
                            slotProps={{ htmlInput: { maxLength: 50 } }}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"><CityIcon color="action" fontSize="small" /></InputAdornment>,
                                sx: { borderRadius: 3 }
                            }}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, md: 4 }}>
                        <TextField
                            label="İlçe"
                            name="stateProvince" required fullWidth
                            value={formData.stateProvince} onChange={handleChange}
                            slotProps={{ htmlInput: { maxLength: 50 } }}
                            InputProps={{ sx: { borderRadius: 3 } }}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, md: 4 }}>
                        <TextField
                            label="Posta Kodu"
                            name="zipCode" fullWidth
                            value={formData.zipCode} onChange={handleChange}
                            slotProps={{ htmlInput: { maxLength: 10 } }}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"><ZipIcon color="action" fontSize="small" /></InputAdornment>,
                                sx: { borderRadius: 3 }
                            }}
                        />
                    </Grid>

                    <Grid size={12}>
                        <TextField
                            label="Açık Adres"
                            name="line1" required multiline rows={2} fullWidth
                            value={formData.line1} onChange={handleChange}
                            placeholder="Mahalle, Cadde, Sokak, Bina No, Kapı No..."
                            spellCheck={false}
                            autoComplete="off"
                            slotProps={{ htmlInput: { maxLength: 250 } }}
                            InputProps={{
                                startAdornment: <InputAdornment position="start" sx={{ mt: 1.5 }}><HomeIcon color="action" fontSize="small" /></InputAdornment>,
                                sx: { borderRadius: 3 }
                            }}
                        />
                    </Grid>

                    <Grid size={12}>
                        <TextField
                            label="Bina / Daire No / Kat (Opsiyonel)"
                            name="line2" fullWidth
                            value={formData.line2} onChange={handleChange}
                            placeholder="Tarif için ek bilgi..."
                            slotProps={{ htmlInput: { maxLength: 250 } }}
                            InputProps={{
                                sx: { borderRadius: 3 },
                                startAdornment: <InputAdornment position="start"><BusinessIcon color="action" fontSize="small" /></InputAdornment>
                            }}
                        />
                    </Grid>

                    <Grid size={{ xs: 12 }}>
                        <TextField
                            label="Ülke"
                            name="country" fullWidth disabled
                            value={formData.country} onChange={handleChange}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"><GlobeIcon color="action" fontSize="small" /></InputAdornment>,
                                sx: { borderRadius: 3, bgcolor: 'action.hover' }
                            }}
                        />
                    </Grid>
                </Grid>

                {(hasOtherAddresses && (!initialData || !initialData.isDefault)) && (
                    <Box sx={{ mt: 2, p: 2, bgcolor: 'primary.50', borderRadius: 3, border: '1px dashed', borderColor: 'primary.main' }}>
                        <FormControlLabel
                            control={<Checkbox checked={formData.isDefault} onChange={handleChange} name="isDefault" color="primary" />}
                            label={<Typography variant="body2" fontWeight="600" color="primary.dark">Bu adresi varsayılan teslimat adresi olarak ayarla</Typography>}
                        />
                    </Box>
                )}
            </DialogContent>

            <DialogActions sx={{ p: 3, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                <Button onClick={onClose} color="inherit" sx={{ textTransform: 'none', borderRadius: 3, px: 3 }}>
                    Vazgeç
                </Button>
                <Button
                    variant="contained"
                    onClick={handleSubmit}
                    disabled={isSubmitting}
                    disableElevation
                    sx={{ textTransform: 'none', borderRadius: 3, px: 5, fontWeight: 'bold' }}
                >
                    {isSubmitting ? 'İşleniyor...' : (initialData ? 'Değişiklikleri Kaydet' : 'Adresi Kaydet')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default AddressFormModal;