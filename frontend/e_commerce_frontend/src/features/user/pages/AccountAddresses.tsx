import React, { useState, useEffect } from 'react';
import { Box, Typography, Button, Grid, Alert, CircularProgress } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import { userService } from '../../../service/userService';
import type { Address, CreateAddressRequest } from '../../../types/user';
import ErrorState from '../../../components/shared/ErrorState';
import AddressCard from '../../../components/customer/AddresCard';
import AddressFormModal from '../../../components/customer/AddressFormModal';

const AccountAddresses: React.FC = () => {
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [settingDefaultId, setSettingDefaultId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);

    const [openModal, setOpenModal] = useState(false);
    const [editingAddress, setEditingAddress] = useState<Address | null>(null);

    const fetchAddresses = async () => {
        try {
            setIsLoading(true);
            const data = await userService.getAddresses();
            const sorted = data.sort((a, b) => (b.isDefault === true ? 1 : 0) - (a.isDefault === true ? 1 : 0));
            setAddresses(sorted);
        } catch (err) {
            setError("Adresler yüklenirken hata oluştu." + err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => { fetchAddresses(); }, []);

    const handleOpenAdd = () => {
        setEditingAddress(null);
        setOpenModal(true);
    };

    const handleOpenEdit = (addr: Address) => {
        setEditingAddress(addr);
        setOpenModal(true);
    };

    const handleFormSubmit = async (formData: CreateAddressRequest) => {
        if (!formData.label || !formData.line1 || !formData.city || !formData.recipientName) {
            alert("Lütfen zorunlu alanları doldurunuz.");
            return;
        }

        try {
            setIsSubmitting(true);

            if (editingAddress) {
                await userService.updateAddress(editingAddress.id, formData);
            } else {
                const payload = {
                    ...formData,
                    isDefault: addresses.length === 0 ? true : formData.isDefault
                };
                await userService.addAddress(payload);
            }

            setOpenModal(false);
            fetchAddresses();
        } catch (err) {
            console.error(err);
            alert("İşlem başarısız oldu.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm("Bu adresi silmek istediğinize emin misiniz?")) return;
        try {
            await userService.deleteAddress(id);
            setAddresses(prev => prev.filter(addr => addr.id !== id));
        } catch (err) { alert("Silinemedi." + err); }
    };

    const handleSetDefault = async (id: number) => {
        try {
            setSettingDefaultId(id);
            await userService.setDefaultAddress(id);
            setAddresses(prev => prev.map(addr => ({ ...addr, isDefault: addr.id === id })));
            fetchAddresses();
        } catch (err) { alert("Güncellenemedi." + err); }
        finally { setSettingDefaultId(null); }
    };

    if (isLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', p: 10 }}><CircularProgress /></Box>;
    if (error) return <ErrorState title="Hata" onRetry={fetchAddresses} />;

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Box>
                    <Typography variant="h5" fontWeight="800" color="primary.main" gutterBottom>
                        Adreslerim
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Teslimat ve fatura adreslerini buradan yönetebilirsin.
                    </Typography>
                </Box>
                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={handleOpenAdd}
                    sx={{ borderRadius: 3, textTransform: 'none', px: 3, boxShadow: 2 }}
                >
                    Yeni Ekle
                </Button>
            </Box>

            {addresses.length === 0 ? (
                <Alert severity="info" sx={{ borderRadius: 2 }}>Henüz kayıtlı bir adresiniz yok.</Alert>
            ) : (
                <Grid container spacing={3}>
                    {addresses.map((addr) => (
                        <AddressCard
                            key={addr.id}
                            address={addr}
                            onEdit={handleOpenEdit}
                            onDelete={handleDelete}
                            onSetDefault={handleSetDefault}
                            isSettingDefault={settingDefaultId === addr.id}
                        />
                    ))}
                </Grid>
            )}

            <AddressFormModal
                open={openModal}
                onClose={() => setOpenModal(false)}
                onSubmit={handleFormSubmit}
                initialData={editingAddress}
                isSubmitting={isSubmitting}
                hasOtherAddresses={addresses.length > 0}
            />
        </Box>
    );
};

export default AccountAddresses;