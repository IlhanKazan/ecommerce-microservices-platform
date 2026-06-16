import React, { useMemo, useState } from 'react';
import {
    Box, Paper, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    TablePagination, Chip, TextField, MenuItem, Stack, Button, Avatar, CircularProgress,
    Switch, InputAdornment, Alert,
} from '@mui/material';
import { Search as SearchIcon, Person as PersonIcon } from '@mui/icons-material';
import { useAdminUsers, useSetUserStatus } from '../../../query/useAdminQueries';
import { useNotification } from '../../../components/shared/NotificationContext';
import EmptyState from '../../../components/shared/EmptyState';

const AdminUsersPage: React.FC = () => {
    const { notify } = useNotification();

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [searchInput, setSearchInput] = useState('');
    const [q, setQ] = useState('');
    const [activeFilter, setActiveFilter] = useState<'' | 'true' | 'false'>('');

    const query = useMemo(
        () => ({ page, size: rowsPerPage, q: q || null, active: activeFilter === '' ? null : activeFilter === 'true' }),
        [page, rowsPerPage, q, activeFilter],
    );
    const { data, isLoading, isError } = useAdminUsers(query);
    const setStatus = useSetUserStatus();

    const applySearch = () => { setPage(0); setQ(searchInput.trim()); };

    const toggle = (id: number, current: boolean) => {
        setStatus.mutate({ id, active: !current }, {
            onSuccess: () => notify(!current ? 'Kullanıcı aktifleştirildi.' : 'Kullanıcı pasifleştirildi.', 'success'),
            onError: (e) => {
                const err = e as { response?: { data?: { message?: string } } };
                notify(err?.response?.data?.message || 'Durum değiştirilemedi.', 'error');
            },
        });
    };

    const users = data?.content ?? [];

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 1 }}>Kullanıcı Yönetimi</Typography>
            <Alert severity="info" sx={{ mb: 3 }}>
                Pasifleştirme platform içi <strong>isActive</strong> bayrağını değiştirir. Keycloak oturum engeli (kalıcı ban)
                ayrı bir özellik olarak planlanmıştır.
            </Alert>

            <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3 }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
                    <TextField
                        size="small" label="E-posta / isim ara" value={searchInput}
                        onChange={(e) => setSearchInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') applySearch(); }}
                        sx={{ flex: 1 }}
                        InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
                    />
                    <TextField
                        size="small" select label="Durum" value={activeFilter}
                        onChange={(e) => { setPage(0); setActiveFilter(e.target.value as '' | 'true' | 'false'); }}
                        sx={{ minWidth: 160 }}
                    >
                        <MenuItem value="">Tümü</MenuItem>
                        <MenuItem value="true">Aktif</MenuItem>
                        <MenuItem value="false">Pasif</MenuItem>
                    </TextField>
                    <Button variant="contained" onClick={applySearch}>Ara</Button>
                </Stack>
            </Paper>

            <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                {isLoading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : isError ? (
                    <EmptyState title="Kullanıcılar yüklenemedi" description="Lütfen daha sonra tekrar deneyin." />
                ) : users.length === 0 ? (
                    <EmptyState title="Kullanıcı bulunamadı" description="Filtrelere uyan kullanıcı yok." />
                ) : (
                    <>
                        <TableContainer>
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Kullanıcı</TableCell>
                                        <TableCell>E-posta</TableCell>
                                        <TableCell>Kayıt</TableCell>
                                        <TableCell>Durum</TableCell>
                                        <TableCell align="right">Aktif</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {users.map((u) => (
                                        <TableRow key={u.id} hover>
                                            <TableCell>
                                                <Stack direction="row" spacing={1.5} alignItems="center">
                                                    <Avatar src={u.profileImageUrl ?? undefined} sx={{ width: 34, height: 34, bgcolor: 'grey.200' }}>
                                                        <PersonIcon fontSize="small" />
                                                    </Avatar>
                                                    <Typography variant="body2" fontWeight={600}>
                                                        {[u.firstName, u.lastName].filter(Boolean).join(' ') || '—'}
                                                    </Typography>
                                                </Stack>
                                            </TableCell>
                                            <TableCell>{u.email}</TableCell>
                                            <TableCell>{new Date(u.createdAt).toLocaleDateString('tr-TR')}</TableCell>
                                            <TableCell>
                                                <Chip size="small" label={u.isActive ? 'Aktif' : 'Pasif'}
                                                      color={u.isActive ? 'success' : 'default'}
                                                      variant={u.isActive ? 'filled' : 'outlined'} />
                                            </TableCell>
                                            <TableCell align="right">
                                                <Switch
                                                    checked={u.isActive}
                                                    onChange={() => toggle(u.id, u.isActive)}
                                                    disabled={setStatus.isPending}
                                                />
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        <TablePagination
                            component="div"
                            count={data?.totalElements ?? 0}
                            page={page}
                            onPageChange={(_, p) => setPage(p)}
                            rowsPerPage={rowsPerPage}
                            onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
                            rowsPerPageOptions={[10, 25, 50]}
                            labelRowsPerPage="Sayfa başına"
                        />
                    </>
                )}
            </Paper>
        </Box>
    );
};

export default AdminUsersPage;
