import React, { useState } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, MenuItem, CircularProgress } from '@mui/material';
import { useGetWarehouses, useAddManualStock } from '../../../query/useProductQueries';

interface AddStockModalProps {
    open: boolean;
    onClose: () => void;
    tenantId: number;
    productId: number;
}

export const AddStockModal = ({ open, onClose, tenantId, productId }: AddStockModalProps) => {
    const [warehouseId, setWarehouseId] = useState<number | ''>('');
    const [amount, setAmount] = useState<number | ''>('');

    const { data: warehouses, isLoading: isWarehousesLoading } = useGetWarehouses(tenantId);
    const { mutate: addStock, isPending } = useAddManualStock(tenantId);

    const handleSubmit = () => {
        if (!warehouseId || !amount) return;

        addStock({ warehouseId: Number(warehouseId), productId, amount: Number(amount) }, {
            onSuccess: () => {
                alert('Stok başarıyla eklendi!');
                onClose();
            },
            onError: () => {
                alert('Stok eklenirken bir hata oluştu.');
            }
        });
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Ürüne Stok Ekle</DialogTitle>
            <DialogContent dividers>
                {isWarehousesLoading ? <CircularProgress /> : (
                    <>
                        <TextField
                            select
                            fullWidth
                            label="Depo Seçiniz"
                            value={warehouseId}
                            onChange={(e) => setWarehouseId(Number(e.target.value))}
                            sx={{ mb: 3, mt: 1 }}
                            required
                        >
                            {warehouses?.length === 0 && (
                                <MenuItem disabled value="">Önce bir depo oluşturmalısınız.</MenuItem>
                            )}
                            {warehouses?.map((w: any) => (
                                <MenuItem key={w.id} value={w.id}>
                                    {w.name} ({w.code})
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            type="number"
                            fullWidth
                            label="Eklenecek Miktar"
                            value={amount}
                            onChange={(e) => setAmount(Number(e.target.value))}
                            required
                            slotProps={{ htmlInput: { min: 1 } }}
                        />
                    </>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} color="inherit">İptal</Button>
                <Button
                    onClick={handleSubmit}
                    variant="contained"
                    disabled={isPending || !warehouseId || !amount}
                >
                    {isPending ? <CircularProgress size={24} color="inherit" /> : 'Kaydet'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};