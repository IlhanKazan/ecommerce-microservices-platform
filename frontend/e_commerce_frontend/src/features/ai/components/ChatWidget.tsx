import React, { useEffect, useRef, useState } from 'react';
import {
    Box, Fab, Paper, IconButton, Typography, TextField, Stack,
    Avatar, CircularProgress, Slide, Chip, Tooltip,
} from '@mui/material';
import {
    Chat as ChatIcon, Close, Send, AutoAwesome, SmartToy,
} from '@mui/icons-material';
import { useChat } from '../../../query/useAiQueries';
import { useAuthStore } from '../../../store/useAuthStore';
import type { ChatUiMessage } from '../types';

const SESSION_KEY = 'ai_chat_session_id';

const WELCOME: ChatUiMessage = {
    role: 'assistant',
    content:
        'Merhaba! 👋 Alışveriş asistanınızım. Ürün arayabilir, sipariş durumunuzu sorabilir ' +
        'veya öneri isteyebilirsiniz. Size nasıl yardımcı olabilirim?',
};

const ChatWidget: React.FC = () => {
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    const [open, setOpen] = useState(false);
    const [input, setInput] = useState('');
    const [messages, setMessages] = useState<ChatUiMessage[]>([WELCOME]);
    const [sessionId, setSessionId] = useState<string | null>(
        () => localStorage.getItem(SESSION_KEY),
    );
    const { mutate: sendChat, isPending } = useChat();
    const listRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (listRef.current) {
            listRef.current.scrollTop = listRef.current.scrollHeight;
        }
    }, [messages, isPending, open]);

    if (!isAuthenticated) return null;

    const handleSend = () => {
        const text = input.trim();
        if (!text || isPending) return;
        setMessages((prev) => [...prev, { role: 'user', content: text }]);
        setInput('');
        sendChat(
            { message: text, sessionId },
            {
                onSuccess: (res) => {
                    if (res.sessionId && res.sessionId !== sessionId) {
                        setSessionId(res.sessionId);
                        localStorage.setItem(SESSION_KEY, res.sessionId);
                    }
                    setMessages((prev) => [
                        ...prev,
                        { role: 'assistant', content: res.message },
                    ]);
                },
                onError: () => {
                    setMessages((prev) => [
                        ...prev,
                        {
                            role: 'assistant',
                            content: 'Üzgünüm, şu an yanıt veremedim. Lütfen tekrar deneyin.',
                        },
                    ]);
                },
            },
        );
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <>
            {!open && (
                <Tooltip title="Alışveriş Asistanı" placement="left">
                    <Fab
                        color="primary"
                        onClick={() => setOpen(true)}
                        sx={{ position: 'fixed', bottom: 24, right: 24, zIndex: 1300 }}
                    >
                        <ChatIcon />
                    </Fab>
                </Tooltip>
            )}

            <Slide direction="up" in={open} mountOnEnter unmountOnExit>
                <Paper
                    elevation={8}
                    sx={{
                        position: 'fixed', bottom: 24, right: 24, zIndex: 1300,
                        width: { xs: 'calc(100vw - 32px)', sm: 380 },
                        height: 540, maxHeight: 'calc(100vh - 48px)',
                        display: 'flex', flexDirection: 'column',
                        borderRadius: 3, overflow: 'hidden',
                    }}
                >
                    {/* Header */}
                    <Box
                        sx={{
                            px: 2, py: 1.5, display: 'flex', alignItems: 'center', gap: 1,
                            bgcolor: 'primary.main', color: 'primary.contrastText',
                        }}
                    >
                        <SmartToy />
                        <Box flex={1}>
                            <Typography variant="subtitle2" fontWeight={800} lineHeight={1.2}>
                                Alışveriş Asistanı
                            </Typography>
                            <Stack direction="row" alignItems="center" spacing={0.5}>
                                <AutoAwesome sx={{ fontSize: 12 }} />
                                <Typography variant="caption">Yapay zeka destekli</Typography>
                            </Stack>
                        </Box>
                        <IconButton size="small" onClick={() => setOpen(false)} sx={{ color: 'inherit' }}>
                            <Close fontSize="small" />
                        </IconButton>
                    </Box>

                    {/* Mesajlar */}
                    <Box
                        ref={listRef}
                        sx={{
                            flex: 1, overflowY: 'auto', p: 1.5,
                            bgcolor: 'grey.50', display: 'flex', flexDirection: 'column', gap: 1,
                        }}
                    >
                        {messages.map((m, i) => (
                            <Stack
                                key={i}
                                direction="row"
                                spacing={1}
                                justifyContent={m.role === 'user' ? 'flex-end' : 'flex-start'}
                            >
                                {m.role === 'assistant' && (
                                    <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main' }}>
                                        <SmartToy sx={{ fontSize: 16 }} />
                                    </Avatar>
                                )}
                                <Box
                                    sx={{
                                        maxWidth: '78%', px: 1.5, py: 1, borderRadius: 2,
                                        bgcolor: m.role === 'user' ? 'primary.main' : 'background.paper',
                                        color: m.role === 'user' ? 'primary.contrastText' : 'text.primary',
                                        boxShadow: 1, whiteSpace: 'pre-wrap', wordBreak: 'break-word',
                                    }}
                                >
                                    <Typography variant="body2" lineHeight={1.55}>
                                        {m.content}
                                    </Typography>
                                </Box>
                            </Stack>
                        ))}
                        {isPending && (
                            <Stack direction="row" spacing={1} alignItems="center">
                                <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main' }}>
                                    <SmartToy sx={{ fontSize: 16 }} />
                                </Avatar>
                                <Chip size="small" icon={<CircularProgress size={12} />} label="Yazıyor…" />
                            </Stack>
                        )}
                    </Box>

                    {/* Input */}
                    <Box sx={{ p: 1.5, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1 }}>
                        <TextField
                            fullWidth size="small" multiline maxRows={3}
                            placeholder="Mesajınızı yazın…"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            disabled={isPending}
                        />
                        <IconButton
                            color="primary"
                            onClick={handleSend}
                            disabled={isPending || !input.trim()}
                            sx={{ alignSelf: 'flex-end' }}
                        >
                            <Send />
                        </IconButton>
                    </Box>
                </Paper>
            </Slide>
        </>
    );
};

export default ChatWidget;
