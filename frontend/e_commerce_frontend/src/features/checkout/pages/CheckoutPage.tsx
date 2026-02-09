import React, { useState } from 'react';
import {
    Box, Typography, Stepper, Step, StepLabel, Button, Paper, Alert,
    Stack, TextField, RadioGroup, FormControlLabel, Radio, FormControl, FormLabel, Divider
} from '@mui/material';

const steps = ['Teslimat Bilgileri', 'Ödeme Yöntemi', 'Siparişi Onayla'];

type StepProps = {
    onNext: () => void;
    onBack?: () => void;
};


const ShippingForm: React.FC<Pick<StepProps, 'onNext'>> = ({ onNext }) => (
    <Box component="form" onSubmit={(e) => { e.preventDefault(); onNext(); }}>
        <Typography variant="h6" gutterBottom>Teslimat Adresi</Typography>
        <Stack spacing={2} sx={{ mt: 2 }}>
            <TextField label="Ad Soyad" fullWidth required />
            <TextField label="Adres Satırı 1" fullWidth required />
            <TextField label="Şehir" fullWidth required />
            <TextField label="Posta Kodu" fullWidth required />
            <Button variant="contained" type="submit" sx={{ mt: 3, alignSelf: 'flex-end' }}>
                Adrese Devam
            </Button>
        </Stack>
    </Box>
);

const PaymentForm: React.FC<StepProps> = ({ onNext, onBack }) => (
    <Box>
        <Typography variant="h6" gutterBottom>Ödeme Yöntemi</Typography>
        <FormControl component="fieldset" fullWidth sx={{ mt: 2 }}>
            <FormLabel component="legend">Yöntem Seçin</FormLabel>
            <RadioGroup name="paymentMethod">
                <FormControlLabel value="creditCard" control={<Radio />} label="Kredi Kartı" />
                <FormControlLabel value="eft" control={<Radio />} label="Havale/EFT" />
                <FormControlLabel value="cod" control={<Radio />} label="Kapıda Ödeme" />
            </RadioGroup>
        </FormControl>
        <Stack direction="row" justifyContent="space-between" sx={{ mt: 3 }}>
            <Button onClick={onBack} variant="outlined">Geri</Button>
            <Button onClick={onNext} variant="contained" color="primary">
                Ödeme Yap
            </Button>
        </Stack>
    </Box>
);

const Review: React.FC<Pick<StepProps, 'onBack'>> = ({ onBack }) => (
    <Box>
        <Typography variant="h6" gutterBottom>Sipariş Özeti ve Onay</Typography>
        <Alert severity="info" sx={{ mb: 2 }}>
            Lütfen teslimat ve ödeme bilgilerinizi kontrol edin. (Simülasyon)
        </Alert>

        <Stack direction="row" justifyContent="space-between" sx={{ mt: 3 }}>
            <Button onClick={onBack} variant="outlined">Geri</Button>
            <Button variant="contained" color="success">
                Siparişi Onayla ve Tamamla
            </Button>
        </Stack>
    </Box>
);


const CheckoutPage: React.FC = () => {
    const [activeStep, setActiveStep] = useState(0);

    const handleNext = () => {
        setActiveStep((prevActiveStep) => prevActiveStep + 1);
    };

    const handleBack = () => {
        setActiveStep((prevActiveStep) => prevActiveStep - 1);
    };

    const getStepContent = (step: number) => {
        switch (step) {
            case 0:
                return <ShippingForm onNext={handleNext} />;
            case 1:
                return <PaymentForm onNext={handleNext} onBack={handleBack} />;
            case 2:
                return <Review onBack={handleBack} />;
            default:
                return <Typography variant="body1">Ödeme tamamlandı!</Typography>;
        }
    };

    return (
        <Box sx={{ py: { xs: 4, md: 8 } }}>
            <Typography variant="h4" component="h1" gutterBottom textAlign="center">
                Ödeme İşlemi
            </Typography>
            <Divider sx={{ mb: 4 }} />

            <Paper elevation={3} sx={{ p: { xs: 2, md: 4 }, maxWidth: 900, mx: 'auto' }}>

                <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
                    {steps.map((label) => (
                        <Step key={label}>
                            <StepLabel>{label}</StepLabel>
                        </Step>
                    ))}
                </Stepper>

                <Box>
                    {getStepContent(activeStep)}
                </Box>

            </Paper>
        </Box>
    );
};

export default CheckoutPage;