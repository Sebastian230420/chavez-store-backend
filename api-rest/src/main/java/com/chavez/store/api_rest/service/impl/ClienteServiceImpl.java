package com.chavez.store.api_rest.service.impl;

import com.chavez.store.api_rest.dto.request.AbonoRequestDTO;
import com.chavez.store.api_rest.dto.request.ClienteRequestDTO;
import com.chavez.store.api_rest.dto.request.LimiteCreditoRequestDTO;
import com.chavez.store.api_rest.dto.response.AbonoResponseDTO;
import com.chavez.store.api_rest.dto.response.ClienteDetalleResponseDTO;
import com.chavez.store.api_rest.dto.response.ClienteResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.VentaResponseDTO;
import com.chavez.store.api_rest.entity.Abono;
import com.chavez.store.api_rest.entity.Cliente;
import com.chavez.store.api_rest.entity.User;
import com.chavez.store.api_rest.entity.Venta;
import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoCliente;
import com.chavez.store.api_rest.entity.enums.TipoVenta;
import com.chavez.store.api_rest.exception.CustomerHasDebtException;
import com.chavez.store.api_rest.exception.DuplicateDocumentException;
import com.chavez.store.api_rest.exception.InvalidPaymentAmountException;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.exception.SaleStateException;
import com.chavez.store.api_rest.repository.dao.IAbonoDAO;
import com.chavez.store.api_rest.repository.dao.IClienteDAO;
import com.chavez.store.api_rest.repository.dao.IUserDAO;
import com.chavez.store.api_rest.repository.dao.IVentaDAO;
import com.chavez.store.api_rest.service.IClienteService;
import com.chavez.store.api_rest.service.IVentaService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clientes, cupos de credito y cobranza.
 * Reglas R-CL-01 a R-CL-11 (LOGICA_NEGOCIO.md seccion 3.6).
 */
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements IClienteService {

    private static final BigDecimal CERO = BigDecimal.ZERO;

    private final IClienteDAO clienteDAO;
    private final IVentaDAO ventaDAO;
    private final IAbonoDAO abonoDAO;
    private final IUserDAO userDAO;
    private final IVentaService ventaService;

    // ══════════════════════════════════════════════════════════
    //  CLIENTES
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ClienteResponseDTO crear(ClienteRequestDTO request) {
        // R-CL-01: documento unico
        if (clienteDAO.existsByDocument(request.getDocument())) {
            throw new DuplicateDocumentException(request.getDocument());
        }

        // R-CL-03: al menos telefono o email
        if (esVacio(request.getPhone()) && esVacio(request.getEmail())) {
            throw new ReglaNegocioException(
                    "El cliente debe tener al menos telefono o email (R-CL-03)");
        }

        Cliente cliente = Cliente.builder()
                .document(request.getDocument())
                .type(request.getType() == null ? TipoCliente.CONSUMIDOR : request.getType())
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .creditLimit(request.getCreditLimit() == null ? CERO : request.getCreditLimit())
                .active(true)
                .build();

        clienteDAO.save(cliente);
        return toResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request) {
        Cliente cliente = obtenerEntidad(id);

        cliente.setFullName(request.getFullName().trim());
        cliente.setPhone(request.getPhone());
        cliente.setEmail(request.getEmail());
        cliente.setAddress(request.getAddress());
        if (request.getType() != null) {
            cliente.setType(request.getType());
        }

        clienteDAO.save(cliente);
        return toResponse(cliente);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Cliente cliente = obtenerEntidad(id);

        // R-CL-08: con saldo pendiente no se elimina, se desactiva
        BigDecimal saldo = clienteDAO.saldoDeudor(id);
        if (saldo.compareTo(CERO) > 0) {
            throw new CustomerHasDebtException(cliente.getFullName(), saldo);
        }

        cliente.setActive(false);
        clienteDAO.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtener(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDetalleResponseDTO obtenerDetalle(Long id) {
        Cliente cliente = obtenerEntidad(id);
        BigDecimal saldo = clienteDAO.saldoDeudor(id);

        List<AbonoResponseDTO> abonos = abonoDAO.findByClienteIdOrderByCreatedAtDesc(id).stream()
                .map(this::toAbonoResponse)
                .toList();

        LocalDateTime ultimaVenta = ventaDAO
                .findByClienteIdAndTypeAndStatusOrderBySaleDateDesc(id, TipoVenta.CREDITO, EstadoVenta.PAGADA)
                .stream()
                .map(Venta::getSaleDate)
                .findFirst()
                .orElse(null);

        LocalDateTime ultimoAbono = abonos.stream()
                .filter(a -> a.getFecha() != null)
                .map(AbonoResponseDTO::getFecha)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ClienteDetalleResponseDTO.builder()
                .id(cliente.getId())
                .document(cliente.getDocument())
                .fullName(cliente.getFullName())
                .creditLimit(cliente.getCreditLimit())
                .saldoActual(saldo)
                .disponible(cliente.getCreditLimit().subtract(saldo))
                .ultimaVenta(ultimaVenta)
                .ultimoAbono(ultimoAbono)
                .ventas(ventaService.listarPorCliente(id))
                .abonos(abonos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ClienteResponseDTO> buscar(String search, TipoCliente tipo, int page, int size) {
        String s = search == null || search.isBlank() ? null : search.trim();
        return PageResponseDTO.of(clienteDAO.buscar(s, tipo,
                        PageRequest.of(page, size, Sort.by("fullName").ascending()))
                .map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarActivos() {
        return clienteDAO.findByActiveTrueOrderByFullNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClienteResponseDTO modificarLimiteCredito(Long id, LimiteCreditoRequestDTO request) {
        Cliente cliente = obtenerEntidad(id);

        BigDecimal limiteAnterior = cliente.getCreditLimit();
        cliente.setCreditLimit(request.getCreditLimit());
        clienteDAO.save(cliente);

        org.slf4j.LoggerFactory.getLogger(ClienteServiceImpl.class)
                .info("Limite de credito de {}: {} -> {}", cliente.getFullName(),
                        limiteAnterior, request.getCreditLimit());

        return toResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal saldoDe(Long clienteId) {
        return clienteDAO.saldoDeudor(clienteId);
    }

    // ══════════════════════════════════════════════════════════
    //  ABONOS
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AbonoResponseDTO registrarAbono(Long clienteId, AbonoRequestDTO request) {
        Cliente cliente = obtenerEntidad(clienteId);

        // R-CL-10: el abono no puede superar la deuda
        BigDecimal saldoAnterior = clienteDAO.saldoDeudor(clienteId);
        if (request.getAmount().compareTo(CERO) <= 0) {
            throw ReglaNegocioException.montoNoPositivo(request.getAmount());
        }
        if (request.getAmount().compareTo(saldoAnterior) > 0) {
            throw new InvalidPaymentAmountException(
                    cliente.getFullName(), request.getAmount(), saldoAnterior);
        }

        Venta ventaAplicada = null;
        // R-CL-11: abono opcional a una venta concreta
        if (request.getAppliedSaleId() != null) {
            ventaAplicada = ventaDAO.findById(request.getAppliedSaleId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Venta", request.getAppliedSaleId()));

            if (!ventaAplicada.getCliente().getId().equals(clienteId)) {
                throw new ReglaNegocioException("La venta no pertenece a este cliente");
            }
            if (ventaAplicada.getType() != TipoVenta.CREDITO) {
                throw SaleStateException.productoInactivo("La venta no es a credito");
            }
            if (ventaAplicada.getStatus() == EstadoVenta.ANULADA) {
                throw SaleStateException.ventaYaAnulada();
            }
        }

        Abono abono = Abono.builder()
                .cliente(cliente)
                .amount(request.getAmount())
                .method(request.getMethod())
                .ventaAplicada(ventaAplicada)
                .notes(request.getNotes())
                .usuario(usuarioActual())
                .build();

        abonoDAO.save(abono);

        BigDecimal saldoActual = saldoAnterior.subtract(request.getAmount());

        return AbonoResponseDTO.builder()
                .id(abono.getId())
                .clienteId(clienteId)
                .clienteNombre(cliente.getFullName())
                .amount(abono.getAmount())
                .method(abono.getMethod())
                .appliedSaleId(ventaAplicada == null ? null : ventaAplicada.getId())
                .appliedSaleDocument(ventaAplicada == null ? null : ventaAplicada.getDocument())
                .notes(abono.getNotes())
                .registradoPor(abono.getUsuario().getUsername())
                .fecha(abono.getCreatedAt())
                .saldoAnterior(saldoAnterior)
                .saldoActual(saldoActual)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbonoResponseDTO> listarAbonos(Long clienteId) {
        obtenerEntidad(clienteId);
        return abonoDAO.findByClienteIdOrderByCreatedAtDesc(clienteId).stream()
                .map(this::toAbonoResponse)
                .toList();
    }

    @Override
    @Transactional
    public AbonoResponseDTO anularAbono(Long clienteId, Long abonoId) {
        Cliente cliente = obtenerEntidad(clienteId);

        Abono abono = abonoDAO.findById(abonoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Abono", abonoId));

        if (!abono.getCliente().getId().equals(clienteId)) {
            throw new ReglaNegocioException("El abono no pertenece a este cliente");
        }
        if (abono.isAnulado()) {
            throw new SaleStateException("El abono ya esta anulado");
        }

        // Al anular, el saldo vuelve a subir: debe seguir cubriéndose (R-CL-09)
        BigDecimal saldoActual = clienteDAO.saldoDeudor(clienteId);
        if (saldoActual.add(abono.getAmount()).compareTo(CERO) < 0) {
            throw new InvalidPaymentAmountException(
                    cliente.getFullName(), abono.getAmount(), saldoActual.negate());
        }

        abono.setAnnulledAt(LocalDateTime.now());
        abono.setAnnulUser(usuarioActual());
        abonoDAO.save(abono);

        return toAbonoResponse(abono);
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private Cliente obtenerEntidad(Long id) {
        return clienteDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", id));
    }

    private User usuarioActual() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userDAO.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.porNombre("Usuario", username));
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private ClienteResponseDTO toResponse(Cliente c) {
        BigDecimal saldo = clienteDAO.saldoDeudor(c.getId());
        BigDecimal limite = c.getCreditLimit() == null ? CERO : c.getCreditLimit();

        return ClienteResponseDTO.builder()
                .id(c.getId())
                .document(c.getDocument())
                .type(c.getType())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .address(c.getAddress())
                .creditLimit(limite)
                .saldoActual(saldo)
                .disponible(limite.subtract(saldo))
                .active(c.getActive())
                .build();
    }

    private AbonoResponseDTO toAbonoResponse(Abono a) {
        return AbonoResponseDTO.builder()
                .id(a.getId())
                .clienteId(a.getCliente().getId())
                .clienteNombre(a.getCliente().getFullName())
                .amount(a.getAmount())
                .method(a.getMethod())
                .appliedSaleId(a.getVentaAplicada() == null ? null : a.getVentaAplicada().getId())
                .appliedSaleDocument(a.getVentaAplicada() == null ? null : a.getVentaAplicada().getDocument())
                .notes(a.getNotes())
                .registradoPor(a.getUsuario() == null ? null : a.getUsuario().getUsername())
                .fecha(a.getCreatedAt())
                .annulledAt(a.getAnnulledAt())
                .build();
    }
}