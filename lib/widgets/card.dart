import 'package:flutter/material.dart';

class TransactionStatusCard extends StatelessWidget {
  final String currentMessage;
  final Map<String, dynamic> currentResult;
  final String currentError;
  final bool isProcessing;

  const TransactionStatusCard({
    Key? key,
    required this.currentMessage,
    required this.currentResult,
    required this.currentError,
    required this.isProcessing,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // Não mostrar se não houver conteúdo
    // if (!isProcessing && currentMessage.isEmpty && currentResult.isEmpty && currentError.isEmpty) {
    //   return const SizedBox.shrink();
    // }

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Card(
        elevation: 2,
        color: _getBackgroundColor(),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(
            color: _getBorderColor(),
            width: 1,
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Ícone e título na mesma linha
              Row(
                children: [
                  _buildStatusIcon(),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      _getTitle(),
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: _getTextColor(),
                      ),
                    ),
                  ),
                ],
              ),

              // Conteúdo
              if (_hasContent()) ...[
                const SizedBox(height: 12),
                _buildContent(),
              ],

              // Loading indicator
              if (isProcessing) ...[
                const SizedBox(height: 12),
                SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(
                      _getTextColor(),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Color _getBackgroundColor() {
    if (currentError.isNotEmpty) {
      return Colors.grey.shade100;
    } else if (currentResult.isNotEmpty) {
      final success = currentResult['success'] as bool? ?? false;
      return success ? Colors.grey.shade50 : Colors.grey.shade100;
    }
    return Colors.grey.shade50;
  }

  Color _getBorderColor() {
    if (currentError.isNotEmpty) {
      return Colors.grey.shade800;
    } else if (currentResult.isNotEmpty) {
      final success = currentResult['success'] as bool? ?? false;
      return success ? Colors.grey.shade700 : Colors.grey.shade600;
    }
    return Colors.grey.shade400;
  }

  Color _getTextColor() {
    if (currentError.isNotEmpty) {
      return Colors.grey.shade900;
    } else if (currentResult.isNotEmpty) {
      return Colors.grey.shade800;
    }
    return Colors.grey.shade700;
  }

  Widget _buildStatusIcon() {
    IconData icon;

    if (currentError.isNotEmpty) {
      icon = Icons.error_outline;
    } else if (currentResult.isNotEmpty) {
      final success = currentResult['success'] as bool? ?? false;
      icon = success ? Icons.check_circle_outline : Icons.cancel_outlined;
    } else if (isProcessing) {
      icon = Icons.sync;
    } else {
      icon = Icons.info_outline;
    }

    return Icon(
      icon,
      size: 24,
      color: _getTextColor(),
    );
  }

  String _getTitle() {
    if (currentError.isNotEmpty) {
      return 'Erro na Transação';
    } else if (currentResult.isNotEmpty) {
      final success = currentResult['success'] as bool? ?? false;
      return success ? 'Transação Aprovada' : 'Transação Recusada';
    } else if (isProcessing) {
      return 'Processando...';
    }
    return 'Aguardando';
  }

  bool _hasContent() {
    return currentMessage.isNotEmpty ||
        currentResult.isNotEmpty ||
        currentError.isNotEmpty;
  }

  Widget _buildContent() {
    if (currentError.isNotEmpty) {
      return Text(
        currentError,
        style: TextStyle(
          fontSize: 13,
          color: Colors.grey.shade700,
        ),
        textAlign: TextAlign.left,
      );
    }

    if (currentResult.isNotEmpty) {
      final resultCode = currentResult['resultCode'] ?? 0;

      return Text(
        'Código de retorno: $resultCode',
        style: TextStyle(
          fontSize: 13,
          color: Colors.grey.shade600,
        ),
      );
    }

    if (currentMessage.isNotEmpty) {
      return Text(
        currentMessage,
        style: TextStyle(
          fontSize: 14,
          color: Colors.grey.shade700,
        ),
        textAlign: TextAlign.left,
        maxLines: 3,
        overflow: TextOverflow.ellipsis,
      );
    }

    return const SizedBox.shrink();
  }
}
