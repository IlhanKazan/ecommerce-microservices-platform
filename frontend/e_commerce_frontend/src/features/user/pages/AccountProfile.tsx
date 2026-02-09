import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
    Box, Typography, TextField, Button, Grid, Avatar, Select, MenuItem, FormControl, InputLabel, Alert, CircularProgress, Stack, Badge, IconButton, type SelectChangeEvent, Paper, Divider
} from '@mui/material';
import {
    Person as PersonIcon,
    Security as SecurityIcon,
    Save as SaveIcon,
    PhotoCamera as PhotoCameraIcon,
    Delete as DeleteIcon,
    Email as EmailIcon,
    Badge as BadgeIcon
} from '@mui/icons-material';
import { userService } from '../../../service/userService';
import type { User, UpdateProfileRequest } from '../../../types/user';
import { useAuthStore } from '../../../store/useAuthStore';
import ErrorState from '../../../components/shared/ErrorState';

const AccountProfile: React.FC = () => {
    const token = useAuthStore((state) => state.token);
    const [dbUser, setDbUser] = useState<User | null>(null);
    const [formData, setFormData] = useState<UpdateProfileRequest>({ phoneNumber: "", profileImageUrl: "", language: "tr" });

    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [saveStatus, setSaveStatus] = useState<'success' | 'error' | null>(null);
    const [loadError, setLoadError] = useState(false);

    const fetchUserData = useCallback(async () => {
        if (!token) return;
        setIsLoading(true);
        setLoadError(false);
        try {
            const user = await userService.getMe();
            setDbUser(user);
            setFormData({
                phoneNumber: user.phoneNumber || "",
                profileImageUrl: user.profileImageUrl || "",
                language: user.language || "tr"
            });
            if (user.profileImageUrl) setPreviewUrl(user.profileImageUrl);
        } catch (error) {
            console.error("Veri çekilemedi:", error);
            setLoadError(true);
        } finally {
            setIsLoading(false);
        }
    }, [token]);

    useEffect(() => { fetchUserData(); }, [fetchUserData]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement> | SelectChangeEvent<string>) => {
        setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }));
        if (saveStatus) setSaveStatus(null);
    };

    const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files[0]) {
            const file = event.target.files[0];
            if (file.size > 10 * 1024 * 1024) { alert("Maksimum dosya boyutu 10MB!"); return; }
            setSelectedFile(file);
            setPreviewUrl(URL.createObjectURL(file));
            setSaveStatus(null);
        }
    };

    const handleRemoveImage = () => {
        setSelectedFile(null);
        setPreviewUrl(null);
        setFormData(prev => ({ ...prev, profileImageUrl: "" }));
        if (fileInputRef.current) fileInputRef.current.value = "";
    };

    const handleRedirectToKeycloak = () => {
        const url = `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}/account`;
        window.open(url, '_blank');
    };

    const handleSave = async () => {
        setIsSaving(true);
        setSaveStatus(null);
        try {
            let finalImageUrl = formData.profileImageUrl;

            if (selectedFile) {
                finalImageUrl = await userService.uploadAvatar(selectedFile);
            } else if (previewUrl === null) {
                finalImageUrl = "";
            }

            const updatedUser = await userService.updateProfile({ ...formData, profileImageUrl: finalImageUrl });

            setDbUser(updatedUser);
            setFormData(prev => ({ ...prev, profileImageUrl: finalImageUrl || "" }));
            setSelectedFile(null);
            setSaveStatus('success');
        } catch (error) {
            console.error("Hata:", error);
            setSaveStatus('error');
        } finally {
            setIsSaving(false);
        }
    };

    if (isLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', p: 10 }}><CircularProgress /></Box>;
    if (loadError || !dbUser) return <ErrorState title="Profil Yüklenemedi" onRetry={fetchUserData} />;

    return (
        <Box>
            <Box sx={{ mb: 4 }}>
                <Typography variant="h5" fontWeight="bold" color="primary.dark">Profil Yönetimi</Typography>
                <Typography variant="body2" color="text.secondary">Hesap bilgilerinizi ve tercihlerinizi buradan düzenleyebilirsiniz.</Typography>
            </Box>

            {saveStatus === 'success' && <Alert severity="success" sx={{ mb: 3 }}>Profiliniz başarıyla güncellendi.</Alert>}
            {saveStatus === 'error' && <Alert severity="error" sx={{ mb: 3 }}>İşlem sırasında bir hata oluştu.</Alert>}

            <Grid container spacing={3}>

                <Grid size={{ xs: 12, md: 4 }}>
                    <Paper elevation={0} sx={{ p: 4, height: '100%', border: '1px solid #e0e0e0', borderRadius: 3, bgcolor: 'white', textAlign: 'center' }}>

                        <Box sx={{ position: 'relative', display: 'inline-block', mb: 2 }}>
                            <Badge
                                overlap="circular"
                                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                                badgeContent={
                                    <Stack direction="row" spacing={0.5}>
                                        <IconButton
                                            color="primary"
                                            onClick={() => fileInputRef.current?.click()}
                                            sx={{ bgcolor: 'white', boxShadow: 2, width: 36, height: 36, '&:hover': { bgcolor: 'grey.100' } }}
                                        >
                                            <PhotoCameraIcon fontSize="small" />
                                        </IconButton>

                                        {previewUrl && (
                                            <IconButton
                                                color="error"
                                                onClick={handleRemoveImage}
                                                sx={{ bgcolor: 'white', boxShadow: 2, width: 36, height: 36, '&:hover': { bgcolor: 'grey.100' } }}
                                            >
                                                <DeleteIcon fontSize="small" />
                                            </IconButton>
                                        )}
                                    </Stack>
                                }
                            >
                                <Avatar
                                    src={previewUrl || undefined}
                                    sx={{
                                        width: 120, height: 120,
                                        bgcolor: 'primary.light',
                                        fontSize: '3rem',
                                        border: '4px solid white',
                                        boxShadow: '0 8px 16px rgba(0,0,0,0.1)'
                                    }}
                                >
                                    {dbUser.firstName?.[0] || <PersonIcon fontSize="inherit" />}
                                </Avatar>
                            </Badge>
                        </Box>

                        <Typography variant="h6" fontWeight="bold" color="text.primary">
                            @{dbUser.username || "kullanici"}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                            {dbUser.email}
                        </Typography>

                        <Divider sx={{ my: 3 }} />

                        <Box sx={{ textAlign: 'left' }}>
                            <Typography variant="caption" fontWeight="bold" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                                KİMLİK BİLGİLERİ
                            </Typography>

                            <Stack spacing={2}>
                                <Box display="flex" alignItems="center" gap={2}>
                                    <BadgeIcon color="action" />
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">Ad Soyad</Typography>
                                        <Typography variant="body2" fontWeight="500">
                                            {dbUser.firstName} {dbUser.lastName}
                                        </Typography>
                                    </Box>
                                </Box>

                                <Box display="flex" alignItems="center" gap={2}>
                                    <EmailIcon color="action" />
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">E-Posta</Typography>
                                        <Typography variant="body2" fontWeight="500">
                                            {dbUser.email}
                                        </Typography>
                                    </Box>
                                </Box>
                            </Stack>

                            <Button
                                variant="outlined"
                                color="inherit"
                                fullWidth
                                startIcon={<SecurityIcon />}
                                onClick={handleRedirectToKeycloak}
                                sx={{ mt: 4, borderRadius: 2, textTransform: 'none', borderColor: 'grey.300' }}
                            >
                                Değiştir
                            </Button>
                        </Box>

                        <input type="file" hidden ref={fileInputRef} accept="image/*" onChange={handleFileSelect} />
                    </Paper>
                </Grid>

                <Grid size={{ xs: 12, md: 8 }}>
                    <Paper elevation={0} sx={{ p: 4, height: '100%', border: '1px solid #e0e0e0', borderRadius: 3, bgcolor: 'white' }}>
                        <Stack direction="row" alignItems="center" gap={1} mb={3}>
                            <SaveIcon color="primary" />
                            <Typography variant="h6" fontWeight="bold">Profil Ayarları</Typography>
                        </Stack>

                        <Stack spacing={4}>
                            <Grid container spacing={3}>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField
                                        label="Cep Telefonu"
                                        name="phoneNumber"
                                        value={formData.phoneNumber}
                                        onChange={handleChange}
                                        fullWidth
                                        variant="outlined"
                                        helperText="Sipariş durumu için kullanılır."
                                    />
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <FormControl fullWidth>
                                        <InputLabel>Dil Tercihi</InputLabel>
                                        <Select
                                            name="language"
                                            value={formData.language || "tr"}
                                            label="Dil Tercihi"
                                            onChange={handleChange}
                                        >
                                            <MenuItem value="tr">Türkçe</MenuItem>
                                            <MenuItem value="en">English</MenuItem>
                                        </Select>
                                    </FormControl>
                                </Grid>
                            </Grid>

                            <Alert severity="info" sx={{ bgcolor: 'info.lighter' }}>
                                Not: Ad ve E-posta değişiklikleri güvenlik nedeniyle sadece Keycloak paneli üzerinden yapılabilir.
                            </Alert>

                            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                                <Button
                                    variant="contained"
                                    size="large"
                                    onClick={handleSave}
                                    disabled={isSaving}
                                    startIcon={isSaving ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
                                    sx={{ px: 5, py: 1.5, borderRadius: 2 }}
                                >
                                    {isSaving ? 'Kaydediliyor...' : 'Değişiklikleri Kaydet'}
                                </Button>
                            </Box>
                        </Stack>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default AccountProfile;