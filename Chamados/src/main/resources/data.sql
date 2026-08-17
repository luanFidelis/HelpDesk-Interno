-- Chamados de demonstracao.
--
-- O registro e append-only: cada linha e uma VERSAO de um chamado, e as linhas que
-- compartilham o mesmo numero_chamado formam a sua linha do tempo. Por isso alguns
-- protocolos abaixo aparecem repetidos, com status e datas diferentes.

-- NC-2026-0001 · aberto, atendido e resolvido (3 versoes)
INSERT INTO chamado (id, numero_chamado, nome, email, descricao, tipo_chamado, prioridade, status, data_abertura, data_fechamento) VALUES
 (RANDOM_UUID(), 'NC-2026-0001', 'Marcos Lima', 'marcos.lima@exemplo.com.br', 'O notebook do setor financeiro nao liga desde a queda de energia de ontem. A luz de carga acende, mas a tela fica preta.', 'HARDWARE', 'ALTA', 'ABERTO', '2026-08-12 08:41:00', NULL),
 (RANDOM_UUID(), 'NC-2026-0001', 'Marcos Lima', 'marcos.lima@exemplo.com.br', 'O notebook do setor financeiro nao liga desde a queda de energia de ontem. A luz de carga acende, mas a tela fica preta.', 'HARDWARE', 'ALTA', 'EM_ANDAMENTO', '2026-08-12 10:15:00', NULL),
 (RANDOM_UUID(), 'NC-2026-0001', 'Marcos Lima', 'marcos.lima@exemplo.com.br', 'O notebook do setor financeiro nao liga desde a queda de energia de ontem. A luz de carga acende, mas a tela fica preta.', 'HARDWARE', 'ALTA', 'RESOLVIDO', '2026-08-13 14:02:00', '2026-08-13 14:02:00');

-- NC-2026-0002 · recem aberto, ainda na fila
INSERT INTO chamado (id, numero_chamado, nome, email, descricao, tipo_chamado, prioridade, status, data_abertura, data_fechamento) VALUES
 (RANDOM_UUID(), 'NC-2026-0002', 'Ana Souza', 'ana.souza@exemplo.com.br', 'Nao consigo enviar e-mails com anexo maior que 5 MB. A mensagem fica presa na caixa de saida e volta com erro de servidor.', 'EMAIL', 'MEDIA', 'ABERTO', '2026-08-16 09:27:00', NULL);

-- NC-2026-0003 · critico, em atendimento
INSERT INTO chamado (id, numero_chamado, nome, email, descricao, tipo_chamado, prioridade, status, data_abertura, data_fechamento) VALUES
 (RANDOM_UUID(), 'NC-2026-0003', 'Juliana Prado', 'juliana.prado@exemplo.com.br', 'Perdi o acesso a pasta compartilhada do departamento de compras. Preciso liberar as notas de hoje e o prazo fecha as 18h.', 'PERMISSOES', 'CRITICA', 'ABERTO', '2026-08-17 07:55:00', NULL),
 (RANDOM_UUID(), 'NC-2026-0003', 'Juliana Prado', 'juliana.prado@exemplo.com.br', 'Perdi o acesso a pasta compartilhada do departamento de compras. Preciso liberar as notas de hoje e o prazo fecha as 18h.', 'PERMISSOES', 'CRITICA', 'EM_ANDAMENTO', '2026-08-17 08:20:00', NULL);

-- NC-2026-0004 · resolvido e reaberto: mostra a data de fechamento voltando a ficar vazia
INSERT INTO chamado (id, numero_chamado, nome, email, descricao, tipo_chamado, prioridade, status, data_abertura, data_fechamento) VALUES
 (RANDOM_UUID(), 'NC-2026-0004', 'Rafael Nunes', 'rafael.nunes@exemplo.com.br', 'A impressora do segundo andar puxa duas folhas por vez e trava no meio do trabalho.', 'IMPRESSORA', 'BAIXA', 'ABERTO', '2026-08-10 13:30:00', NULL),
 (RANDOM_UUID(), 'NC-2026-0004', 'Rafael Nunes', 'rafael.nunes@exemplo.com.br', 'A impressora do segundo andar puxa duas folhas por vez e trava no meio do trabalho.', 'IMPRESSORA', 'BAIXA', 'RESOLVIDO', '2026-08-11 16:45:00', '2026-08-11 16:45:00'),
 (RANDOM_UUID(), 'NC-2026-0004', 'Rafael Nunes', 'rafael.nunes@exemplo.com.br', 'A impressora do segundo andar puxa duas folhas por vez e trava no meio do trabalho. Voltou a travar hoje, mesmo depois da troca do rolete.', 'IMPRESSORA', 'BAIXA', 'REABERTO', '2026-08-16 11:08:00', NULL);

-- NC-2026-0005 · aberto
INSERT INTO chamado (id, numero_chamado, nome, email, descricao, tipo_chamado, prioridade, status, data_abertura, data_fechamento) VALUES
 (RANDOM_UUID(), 'NC-2026-0005', 'Camila Rocha', 'camila.rocha@exemplo.com.br', 'O sistema de estoque esta lento para gravar entrada de mercadoria. Cada lancamento leva quase um minuto para confirmar.', 'SISTEMA', 'ALTA', 'ABERTO', '2026-08-17 10:12:00', NULL);
