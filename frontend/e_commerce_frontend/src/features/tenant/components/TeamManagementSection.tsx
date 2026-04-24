import React, { useState } from 'react';
import {
    Box, Paper, Typography, Button, Avatar, Chip,
    Stack, IconButton, Dialog, DialogTitle,
    DialogContent, DialogActions, TextField, MenuItem,
    Alert, CircularProgress, Menu, Tooltip, DialogContentText
} from '@mui/material';
import {
    GroupAdd as GroupAddIcon,
    Group as GroupIcon,
    PersonRemove as RemoveIcon,
    Badge as BadgeIcon,
    Edit as EditIcon,
    Warning as WarningIcon
} from '@mui/icons-material';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { tenantService } from '../api/tenantService.ts';
import { TENANT_ROLES, type TenantMember, type AddMemberRequest, type TenantRole } from '../../../types/tenant';
import { useNotification } from '../../../components/shared/NotificationProvider';

interface TeamManagementSectionProps {
    tenantId: number;
    members: TenantMember[];
}

const TeamManagementSection: React.FC<TeamManagementSectionProps> = ({ tenantId, members }) => {
    const queryClient = useQueryClient();

    const [openAddModal, setOpenAddModal] = useState(false);
    const [addFormData, setAddFormData] = useState<AddMemberRequest>({ email: '', role: 'STAFF' });
    const [addErrorMsg, setAddErrorMsg] = useState<string | null>(null);

    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);

    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const [pendingRole, setPendingRole] = useState<TenantRole | null>(null);

    const { notify } = useNotification();

    const [confirmRemoveDialog, setConfirmRemoveDialog] = useState<{ open: boolean; memberId: number | null }>({
        open: false,
        memberId: null
    });

    const addMemberMutation = useMutation({
        mutationFn: async () => {
            return tenantService.addMember(tenantId, addFormData);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
            setOpenAddModal(false);
            setAddFormData({ email: '', role: 'STAFF' });
            setAddErrorMsg(null);
            notify('Personel başarıyla eklendi.', 'success');
        },
        onError: (err: any) => {
            const message = err.response?.data?.message || "Ekleme sırasında hata oluştu.";
            setAddErrorMsg(message);
        }
    });

    const removeMemberMutation = useMutation({
        mutationFn: async (memberId: number) => {
            return tenantService.removeMember(tenantId, memberId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
            notify('Personel başarıyla silindi.', 'success');
        },
        onError: () => {
            notify('Silme işlemi başarısız oldu.', 'error');
        }
    });

    const updateRoleMutation = useMutation({
        mutationFn: async ({ memberId, newRole }: { memberId: number, newRole: TenantRole }) => {
            return tenantService.updateMemberRole(tenantId, memberId, newRole);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
            notify('Rol başarıyla güncellendi.', 'success');
            resetRoleStates();
        },
        onError: () => {
            notify('Rol güncelleme başarısız oldu.', 'error');
            resetRoleStates();
        }
    });

    const resetRoleStates = () => {
        setConfirmDialogOpen(false);
        setPendingRole(null);
        setSelectedMemberId(null);
        setAnchorEl(null);
    };

    const handleRemoveClick = (memberId: number) => {
        setConfirmRemoveDialog({ open: true, memberId });
    };

    const handleRoleEditClick = (event: React.MouseEvent<HTMLElement>, memberId: number) => {
        event.stopPropagation();
        setAnchorEl(event.currentTarget);
        setSelectedMemberId(memberId);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const handleRoleSelect = (newRole: TenantRole) => {
        setPendingRole(newRole);
        setAnchorEl(null);
        setConfirmDialogOpen(true);
    };

    const handleConfirmRoleChange = () => {
        if (!selectedMemberId || !pendingRole) {
            notify('Lütfen önce bir personel ve rol seçin.', 'warning');
            return;
        }

        updateRoleMutation.mutate({ memberId: selectedMemberId, newRole: pendingRole });
    };

    const handleConfirmRemove = () => {
        if (confirmRemoveDialog.memberId) {
            removeMemberMutation.mutate(confirmRemoveDialog.memberId);
        }
        setConfirmRemoveDialog({ open: false, memberId: null });
    };

    const handleCancelRemove = () => {
        setConfirmRemoveDialog({ open: false, memberId: null });
    };

    const handleAddClose = () => {
        setOpenAddModal(false);
        setAddErrorMsg(null);
    };

    const targetMember = members.find(m => m.memberId === selectedMemberId);

    return (
        <Paper sx={{ p: 3, borderRadius: 4 }} variant="outlined">
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <GroupIcon color="primary" /> Ekip Yönetimi ({members.length})
                </Typography>
                <Button
                    variant="contained"
                    startIcon={<GroupAddIcon />}
                    size="small"
                    onClick={() => setOpenAddModal(true)}
                    sx={{ borderRadius: 2 }}
                >
                    Personel Ekle
                </Button>
            </Box>

            <Stack spacing={2}>
                {members.map((member) => (
                    <Box
                        key={member.memberId}
                        sx={{
                            p: 2,
                            borderRadius: 3,
                            bgcolor: '#f8fafc',
                            border: '1px solid #e2e8f0',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 2
                        }}
                    >
                        <Avatar
                            src={member.profileImageUrl || undefined}
                            sx={{ width: 48, height: 48, bgcolor: 'secondary.main' }}
                        >
                            {member.firstName.charAt(0).toUpperCase()}
                        </Avatar>

                        <Box sx={{ flexGrow: 1 }}>
                            <Typography variant="subtitle2" fontWeight="bold">
                                {member.firstName} {member.lastName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                {member.email}
                            </Typography>
                        </Box>

                        <Stack direction="row" alignItems="center" gap={1}>
                            <Chip
                                label={member.role}
                                size="small"
                                color={member.role === 'OWNER' ? 'warning' : 'primary'}
                                variant={member.role === 'OWNER' ? 'filled' : 'outlined'}
                            />

                            {member.role !== 'OWNER' && (
                                <>
                                    <Tooltip title="Rolü Değiştir">
                                        <IconButton
                                            size="small"
                                            onClick={(e) => handleRoleEditClick(e, member.memberId)}
                                            disabled={updateRoleMutation.isPending}
                                        >
                                            <EditIcon fontSize="small" />
                                        </IconButton>
                                    </Tooltip>

                                    <Tooltip title="Ekipten Çıkar">
                                        <IconButton
                                            size="small"
                                            color="error"
                                            onClick={() => handleRemoveClick(member.memberId)}
                                            disabled={removeMemberMutation.isPending}
                                        >
                                            {removeMemberMutation.isPending && removeMemberMutation.variables === member.memberId ?
                                                <CircularProgress size={16} color="error" /> :
                                                <RemoveIcon fontSize="small" />
                                            }
                                        </IconButton>
                                    </Tooltip>
                                </>
                            )}
                        </Stack>
                    </Box>
                ))}
            </Stack>

            <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={handleMenuClose}
                PaperProps={{ sx: { minWidth: 150, borderRadius: 2 } }}
            >
                {TENANT_ROLES.map((roleOpt) => (
                    <MenuItem
                        key={roleOpt.value}
                        onClick={() => handleRoleSelect(roleOpt.value)}
                        selected={members.find(m => m.memberId === selectedMemberId)?.role === roleOpt.value}
                    >
                        {roleOpt.label}
                    </MenuItem>
                ))}
            </Menu>

            <Dialog
                open={confirmDialogOpen}
                onClose={resetRoleStates}
                maxWidth="xs"
                fullWidth
            >
                <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <WarningIcon color="warning" /> Rol Değişikliği
                </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        <b>{targetMember?.firstName} {targetMember?.lastName}</b> adlı personelin rolünü{' '}
                        <Box component="span" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                            {TENANT_ROLES.find(r => r.value === pendingRole)?.label}
                        </Box>{' '}
                        olarak güncellemek istediğinize emin misiniz?
                    </DialogContentText>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={resetRoleStates} color="inherit">
                        Vazgeç
                    </Button>
                    <Button
                        onClick={handleConfirmRoleChange}
                        variant="contained"
                        color="warning"
                        autoFocus
                        disabled={updateRoleMutation.isPending}
                    >
                        {updateRoleMutation.isPending ? 'Güncelleniyor...' : 'Evet, Değiştir'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={openAddModal} onClose={handleAddClose} fullWidth maxWidth="xs">
                <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <BadgeIcon color="primary" /> Yeni Personel Ekle
                </DialogTitle>
                <DialogContent dividers>
                    {addErrorMsg && <Alert severity="error" sx={{ mb: 2 }}>{addErrorMsg}</Alert>}
                    <Alert severity="info" sx={{ mb: 3 }}>
                        Eklemek istediğiniz kişinin sistemde kayıtlı olması gerekmektedir.
                    </Alert>
                    <Stack spacing={2}>
                        <TextField
                            label="Kullanıcı E-posta Adresi"
                            fullWidth
                            type="email"
                            required
                            placeholder="ornek@gmail.com"
                            value={addFormData.email}
                            onChange={(e) => setAddFormData({ ...addFormData, email: e.target.value })}
                        />
                        <TextField
                            select
                            label="Atanacak Rol"
                            fullWidth
                            value={addFormData.role}
                            onChange={(e) => setAddFormData({ ...addFormData, role: e.target.value as TenantRole })}
                        >
                            {TENANT_ROLES.map((option) => (
                                <MenuItem key={option.value} value={option.value}>
                                    {option.label}
                                </MenuItem>
                            ))}
                        </TextField>
                    </Stack>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={handleAddClose} color="inherit" disabled={addMemberMutation.isPending}>
                        Vazgeç
                    </Button>
                    <Button
                        variant="contained"
                        onClick={() => addMemberMutation.mutate()}
                        disabled={addMemberMutation.isPending || !addFormData.email}
                        startIcon={addMemberMutation.isPending ? <CircularProgress size={20} color="inherit"/> : <GroupAddIcon />}
                    >
                        Davet Et
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Confirm Remove Dialog */}
            <Dialog
                open={confirmRemoveDialog.open}
                onClose={handleCancelRemove}
                maxWidth="sm"
                fullWidth
            >
                <DialogTitle sx={{ fontWeight: 'bold' }}>
                    Personeli Çıkar
                </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Bu personeli ekipten çıkarmak istediğinize emin misiniz? Bu işlem geri alınamaz.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCancelRemove} color="inherit">
                        İptal
                    </Button>
                    <Button 
                        onClick={handleConfirmRemove} 
                        color="error" 
                        variant="contained"
                        disabled={removeMemberMutation.isPending}
                    >
                        {removeMemberMutation.isPending ? 'Çıkarılıyor...' : 'Çıkar'}
                    </Button>
                </DialogActions>
            </Dialog>


        </Paper>
    );
};

export default TeamManagementSection;