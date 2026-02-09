import React from 'react';
import {
    Typography,
    Box,
    Divider,
    Paper,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    Stack
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import PhoneIcon from '@mui/icons-material/Phone';
import EmailIcon from '@mui/icons-material/Email';

const ContactPage: React.FC = () => {
    return (
        <Box sx={{ py: { xs: 4, md: 8 } }}>
            <Typography variant="h3" component="h1" gutterBottom>
                Bize Ulaşın
            </Typography>
            <Divider sx={{ mb: 4 }} />

            <Stack
                spacing={4}
                direction={{ xs: 'column', md: 'row' }}
                alignItems="stretch"
            >

                <Box sx={{ flex: 1 }}>
                    <Paper elevation={3} sx={{ p: 4, height: '100%' }}>
                        <Typography variant="h5" component="h2" gutterBottom>
                            İletişim Detayları
                        </Typography>
                        <List>
                            <ListItem>
                                <ListItemIcon><LocationOnIcon color="primary" /></ListItemIcon>
                                <ListItemText primary="Adres" secondary="Süleymaniye Mah. 1800. Sok. No: 9, Şanlıurfa/Türkiye" />
                            </ListItem>
                            <ListItem>
                                <ListItemIcon><PhoneIcon color="primary" /></ListItemIcon>
                                <ListItemText primary="Telefon" secondary="+90 (555) 555 55 55" />
                            </ListItem>
                            <ListItem>
                                <ListItemIcon><EmailIcon color="primary" /></ListItemIcon>
                                <ListItemText primary="E-posta" secondary="info@ilhaneticaret.com" />
                            </ListItem>
                        </List>
                    </Paper>
                </Box>

                <Box sx={{ flex: 1 }}>
                    <Box
                        sx={{
                            height: 300,
                            bgcolor: 'grey.300',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center'
                        }}
                    >
                        <Typography variant="subtitle1" color="text.secondary">
                            Harita Yer Tutucusu (Örn: Google Maps)
                        </Typography>
                    </Box>
                </Box>

            </Stack>
        </Box>
    );
};

export default ContactPage;