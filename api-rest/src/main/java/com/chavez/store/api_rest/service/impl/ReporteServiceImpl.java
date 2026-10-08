package com.chavez.store.api_rest.service.impl;

import com.chavez.store.api_rest.dto.response.AbonoDelDiaDTO;
import com.chavez.store.api_rest.dto.response.Antiguedad;
import com.chavez.store.api_rest.dto.response.CategoriaGananciaDTO;
import com.chavez.store.api_rest.dto.response.ClienteDeudaDTO;
import com.chavez.store.api_rest.dto.response.CuentasPorCobrarResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciaDiaDTO;
import com.chavez.store.api_rest.dto.response.GananciaTipoVentaDTO;
import com.chavez.store.api_rest.dto.response.GananciasCategoriaResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasResponseDTO;
import com.chavez.store.api_rest.dto.response.LotePorVencerDTO;
import com.chavez.store.api_rest.dto.response.MermaMotivoDTO;
import com.chavez.store.api_rest.dto.response.MermaProductoDTO;
import com.chavez.store.api_rest.dto.response.MermasResponseDTO;
import com.chavez.store.api_rest.dto.response.ProductoGananciaDTO;
import com.chavez.store.api_rest.dto.response.ResumenCobranza;
import com.chavez.store.api_rest.dto.response.ResumenDia;
import com.chavez.store.api_rest.dto.response.ResumenStock;
import com.chavez.store.api_rest.dto.response.StockCriticoDTO;
import com.chavez.store.api_rest.dto.response.StockReporteResponseDTO;
import com.chavez.store.api_rest.dto.response.TopProductoDTO;
import com.chavez.store.api_rest.dto.response.Totales;
import com.chavez.store.api_rest.dto.response.VentaHoraDTO;
import com.chavez.store.api_rest.dto.response.VentasDelDiaResponseDTO;
import com.chavez.store.api_rest.dto.response.VentasPorHoraResponseDTO;
import com.chavez.store.api_rest.entity.Abono;
import com.chavez.store.api_rest.entity.Cliente;
import com.chavez.store.api_rest.entity.Producto;
import com.chavez.store.api_rest.entity.Venta;
import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;
import com.chavez.store.api_rest.entity.enums.TipoVenta;
import com.chavez.store.api_rest.repository.dao.IAbonoDAO;
import com.chavez.store.api_rest.repository.dao.IClienteDAO;
import com.chavez.store.api_rest.repository.dao.IProductoDAO;
import com.chavez.store.api_rest.repository.dao.IReporteDAO;
import com.chavez.store.api_rest.repository.dao.StockVencimientoRow;
import com.chavez.store.api_rest.repository.dao.IVentaDAO;
import com.chavez.store.api_rest.service.IReporteService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reportes de ganancias, mermas, stock y cobranza.
 * Reglas R-R-01 a R-R-10 (LOGICA_NEGOCIO.md seccion 3.8).
 *
 * Todos los reportes excluyen ventas ANULADAS (R-R-01) y usan el snapshot
 * de costo de sale_details, nunca el costo actual del producto (R-R-03).
 */
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements IReporteService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CERO = BigDecimal.ZERO;

    private final IReporteDAO reporteDAO;
    private final IVentaDAO ventaDAO;
    private final IClienteDAO clienteDAO;
    private final IAbonoDAO abonoDAO;
    private final IProductoDAO productoDAO;

    // ══════════════════════════════════════════════════════════
    //  GANANCIAS POR PERIODO
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public GananciasResponseDTO ganancias(LocalDate desde, LocalDate hasta) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hasta.atTime(LocalTime.MAX);

        BigDecimal ingresos = CERO;
        BigDecimal costo = CERO;
        long unidades = 0;
        Map<LocalDate, AcumuladoDia> porDia = new LinkedHashMap<>();

        for (Object[] fila : reporteDAO.gananciasPorDia(d, h)) {
            LocalDate fecha = toLocalDate(fila[0]);
            long ventasCount = toLong(fila[1]);
            long uni = toLong(fila[2]);
            BigDecimal ing = toDecimal(fila[3]);
            BigDecimal cos = toDecimal(fila[4]);

            ingresos = ingresos.add(ing);
            costo = costo.add(cos);
            unidades += uni;

            porDia.put(fecha, new AcumuladoDia(fecha, ventasCount, uni, ing, cos, CERO));
        }

        // Mermas del periodo (no dependen de que el producto se haya vendido)
        BigDecimal mermas = CERO;
        for (Object[] fila : reporteDAO.mermasPorMotivo(d, h)) {
            mermas = mermas.add(toDecimal(fila[2]));
        }

        // Las mermas se cruzan por dia en consulta aparte (MySQL 9 exige ONLY_FULL_GROUP_BY,
        // que invalida la subconsulta correlacionada sobre DATE(s.sale_date)).
        for (Object[] fila : reporteDAO.mermasPorDia(d, h)) {
            LocalDate fecha = toLocalDate(fila[0]);
            BigDecimal costoMerma = toDecimal(fila[2]);
            AcumuladoDia existente = porDia.get(fecha);
            if (existente == null) {
                porDia.put(fecha, new AcumuladoDia(fecha, 0, 0, CERO, CERO, costoMerma));
            } else {
                porDia.put(fecha, new AcumuladoDia(fecha, existente.ventasCount, existente.unidades,
                        existente.ingresos, existente.costo, costoMerma));
            }
        }

        // R-R-02: utilidad = ingresos - costo - mermas
        BigDecimal utilidad = ingresos.subtract(costo).subtract(mermas);

        List<GananciaDiaDTO> dias = porDia.values().stream()
                .sorted(java.util.Comparator.comparing(AcumuladoDia::fecha))
                .map(a -> GananciaDiaDTO.builder()
                        .fecha(a.fecha)
                        .ventasCount(a.ventasCount)
                        .unidadesVendidas(a.unidades)
                        .ingresos(a.ingresos)
                        .costo(a.costo)
                        .mermas(a.mermas)
                        .utilidad(a.ingresos.subtract(a.costo).subtract(a.mermas))
                        .margenPct(margen(a.ingresos.subtract(a.costo), a.ingresos))
                        .build())
                .toList();

        Totales totales = Totales.builder()
                .ventasCount(porDia.values().stream().mapToLong(a -> a.ventasCount).sum())
                .unidadesVendidas(unidades)
                .ingresos(redondear(ingresos))
                .costoVentas(redondear(costo))
                .mermas(redondear(mermas))
                .utilidadNeta(redondear(utilidad))
                .margenPct(margen(ingresos.subtract(costo), ingresos))
                .margenRealPct(margen(utilidad, ingresos))
                .utilidadPorUnidad(unidades == 0 ? null
                        : redondear(utilidad.divide(BigDecimal.valueOf(unidades), 4, RoundingMode.HALF_UP)))
                .build();

        // R-R-01: por tipo de venta
        List<Venta> ventas = ventaDAO.buscar(EstadoVenta.PAGADA, null, null, d, h,
                        org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();

        Map<TipoVenta, long[]> conteo = new LinkedHashMap<>();
        Map<TipoVenta, BigDecimal> montos = new LinkedHashMap<>();
        for (Venta v : ventas) {
            conteo.merge(v.getType(), new long[]{1}, (a, b) -> new long[]{a[0] + b[0]});
            montos.merge(v.getType(), v.getTotal(), BigDecimal::add);
        }

        List<GananciaTipoVentaDTO> porTipo = new ArrayList<>();
        for (Map.Entry<TipoVenta, long[]> e : conteo.entrySet()) {
            BigDecimal total = montos.get(e.getKey());
            porTipo.add(GananciaTipoVentaDTO.builder()
                    .type(e.getKey())
                    .ventasCount(e.getValue()[0])
                    .total(redondear(total))
                    .porcentaje(porcentaje(total, ingresos))
                    .build());
        }

        return GananciasResponseDTO.builder()
                .desde(desde)
                .hasta(hasta)
                .totales(totales)
                .porDia(dias)
                .porTipoVenta(porTipo)
                .recaudadoAbonos(redondear(abonoDAO.recaudadoEntre(d, h)))
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  GANANCIAS POR CATEGORIA
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public GananciasCategoriaResponseDTO gananciasPorCategoria(LocalDate desde, LocalDate hasta) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hasta.atTime(LocalTime.MAX);

        List<CategoriaGananciaDTO> categorias = new ArrayList<>();
        BigDecimal totalIngresos = CERO;
        BigDecimal totalUtilidad = CERO;

        List<Object[]> filas = reporteDAO.gananciasPorCategoria(d, h);

        // El total del periodo se calcula aparte para el porcentaje de participacion
        BigDecimal ingresosPeriodo = CERO;
        for (Object[] fila : filas) {
            ingresosPeriodo = ingresosPeriodo.add(toDecimal(fila[5]));
        }

        for (Object[] fila : filas) {
            BigDecimal ingresos = toDecimal(fila[5]);
            BigDecimal costo = toDecimal(fila[6]);
            BigDecimal mermas = toDecimal(fila[7]);
            BigDecimal utilidad = ingresos.subtract(costo).subtract(mermas);

            totalIngresos = totalIngresos.add(ingresos);
            totalUtilidad = totalUtilidad.add(utilidad);

            categorias.add(CategoriaGananciaDTO.builder()
                    .categoriaId(toLong(fila[0]))
                    .categoriaNombre((String) fila[1])
                    .ventasCount(toLong(fila[2]))
                    .unidadesVendidas(toLong(fila[3]))
                    .litrosVendidos(fila[4] == null ? null : ((Number) fila[4]).doubleValue())
                    .ingresos(redondear(ingresos))
                    .costoVentas(redondear(costo))
                    .mermas(redondear(mermas))
                    .utilidad(redondear(utilidad))
                    // R-R-05: teorico vs real
                    .margenTeoricoPct(margen(ingresos.subtract(costo), ingresos))
                    .margenRealPct(margen(utilidad, ingresos))
                    .participacionIngresosPct(porcentaje(ingresos, ingresosPeriodo))
                    .build());
        }

        return GananciasCategoriaResponseDTO.builder()
                .desde(desde)
                .hasta(hasta)
                .totalIngresos(redondear(totalIngresos))
                .totalUtilidad(redondear(totalUtilidad))
                .categorias(categorias)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GananciasProductoResponseDTO gananciasPorProducto(LocalDate desde, LocalDate hasta,
                                                            Long categoriaId) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hasta.atTime(LocalTime.MAX);

        List<ProductoGananciaDTO> productos = new ArrayList<>();
        for (Object[] fila : reporteDAO.gananciasPorProducto(d, h, categoriaId)) {
            BigDecimal ingresos = toDecimal(fila[5]);
            BigDecimal costo = toDecimal(fila[6]);
            BigDecimal mermas = toDecimal(fila[7]);
            BigDecimal utilidad = ingresos.subtract(costo).subtract(mermas);

            productos.add(ProductoGananciaDTO.builder()
                    .productoId(toLong(fila[0]))
                    .sku((String) fila[1])
                    .nombre((String) fila[2])
                    .categoria((String) fila[3])
                    .unidadesVendidas(toLong(fila[4]))
                    .ingresos(redondear(ingresos))
                    .costo(redondear(costo))
                    .mermas(redondear(mermas))
                    .utilidad(redondear(utilidad))
                    .margenTeoricoPct(margen(ingresos.subtract(costo), ingresos))
                    .margenRealPct(margen(utilidad, ingresos))
                    .build());
        }

        return GananciasProductoResponseDTO.builder()
                .desde(desde)
                .hasta(hasta)
                .totalProductos(productos.size())
                .productos(productos)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  MERMAS
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public MermasResponseDTO mermas(LocalDate desde, LocalDate hasta, String motivo) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hasta.atTime(LocalTime.MAX);

        List<MermaMotivoDTO> porMotivo = new ArrayList<>();
        long unidades = 0;
        BigDecimal costoTotal = CERO;

        for (Object[] fila : reporteDAO.mermasPorMotivo(d, h)) {
            String m = (String) fila[0];
            if (motivo != null && !motivo.isBlank() && !motivo.equalsIgnoreCase(m)) {
                continue;
            }
            long u = toLong(fila[1]);
            BigDecimal costo = toDecimal(fila[2]);
            unidades += u;
            costoTotal = costoTotal.add(costo);
            porMotivo.add(MermaMotivoDTO.builder()
                    .motivo(m)
                    .unidades(u)
                    .costoPerdido(redondear(costo))
                    .porcentaje(porcentaje(costo, costoTotal))
                    .build());
        }

        List<MermaProductoDTO> porProducto = new ArrayList<>();
        for (Object[] fila : reporteDAO.mermasPorProducto(d, h)) {
            BigDecimal costo = toDecimal(fila[3]);
            porProducto.add(MermaProductoDTO.builder()
                    .productoId(toLong(fila[0]))
                    .nombre((String) fila[1])
                    .unidades(toLong(fila[2]))
                    .costoPerdido(redondear(costo))
                    .mermaSobreVentaPct(null)
                    .build());
        }

        return MermasResponseDTO.builder()
                .desde(desde)
                .hasta(hasta)
                .unidades(unidades)
                .costoPerdido(redondear(costoTotal))
                .porMotivo(porMotivo)
                .porProducto(porProducto)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  STOCK
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public StockReporteResponseDTO stock(LocalDate desde, LocalDate hasta) {
        // R-I-09: productos en o por debajo del minimo
        List<StockCriticoDTO> critico = productoDAO.findStockCritico().stream()
                .map(p -> StockCriticoDTO.builder()
                        .productoId(p.getId())
                        .sku(p.getSku())
                        .nombre(p.getName())
                        .stock(p.getStock())
                        .stockMin(p.getMinStock())
                        .faltante(Math.max(0, p.getMinStock() - p.getStock()))
                        .build())
                .toList();

        // R-R-07: lotes que vencen en el rango
        List<LotePorVencerDTO> porVencer = new ArrayList<>();
        BigDecimal valorPorVencer = CERO;

        for (StockVencimientoRow fila : reporteDAO.lotesPorVencer(desde, hasta)) {
            BigDecimal valor = BigDecimal.valueOf(fila.getQtyRemaining())
                    .multiply(toDecimal(fila.getCostoUnit()));
            valorPorVencer = valorPorVencer.add(valor);

            porVencer.add(LotePorVencerDTO.builder()
                    .productoId(fila.getProductoId())
                    .sku(fila.getSku())
                    .nombre(fila.getNombre())
                    .lotCode(fila.getSku())
                    .qtyRemaining(fila.getQtyRemaining())
                    .expiryDate(fila.getExpiryDate())
                    .diasRestantes(ChronoUnit.DAYS.between(LocalDate.now(), fila.getExpiryDate()))
                    .valor(redondear(valor))
                    .build());
        }

        long sinStock = productoDAO.findByActiveTrueOrderByNameAsc().stream()
                .filter(p -> p.getStock() == 0)
                .count();

        return StockReporteResponseDTO.builder()
                .stockCritico(critico)
                .porVencer(porVencer)
                .resumen(ResumenStock.builder()
                        .productosStockCritico(critico.size())
                        .productosSinStock((int) sinStock)
                        .lotesPorVencer(porVencer.size())
                        .lotesVencidos(0)
                        .valorPorVencer(redondear(valorPorVencer))
                        .valorVencido(CERO)
                        .build())
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  COBRANZA
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public CuentasPorCobrarResponseDTO cuentasPorCobrar() {
        LocalDateTime hoy = LocalDateTime.now();

        // Deuda por venta a credito vigente
        Map<Long, List<Object[]>> porCliente = new LinkedHashMap<>();
        for (Object[] fila : reporteDAO.antiguedadDeuda(hoy)) {
            long clienteId = toLong(fila[0]);
            porCliente.computeIfAbsent(clienteId, k -> new ArrayList<>()).add(fila);
        }

        List<ClienteDeudaDTO> clientes = new ArrayList<>();
        BigDecimal saldoTotal = CERO;
        BigDecimal mas30 = CERO;
        BigDecimal mas90 = CERO;

        for (Map.Entry<Long, List<Object[]>> e : porCliente.entrySet()) {
            Cliente cliente = clienteDAO.findById(e.getKey()).orElse(null);
            if (cliente == null) {
                continue;
            }

            BigDecimal saldoCliente = clienteDAO.saldoDeudor(cliente.getId());
            if (saldoCliente.compareTo(CERO) <= 0) {
                continue;
            }

            BigDecimal sumaVentas = e.getValue().stream()
                    .map(f -> toDecimal(f[2]))
                    .reduce(CERO, BigDecimal::add);

            // Reparte el saldo pendiente proporcionalmente a cada venta por antiguedad
            BigDecimal d0a30 = CERO;
            BigDecimal d31a60 = CERO;
            BigDecimal d61a90 = CERO;
            BigDecimal mas90d = CERO;

            for (Object[] fila : e.getValue()) {
                long dias = toLong(fila[3]);
                BigDecimal porcion = porcentajeDe(saldoCliente, toDecimal(fila[2]), sumaVentas);

                if (dias <= 30) {
                    d0a30 = d0a30.add(porcion);
                } else if (dias <= 60) {
                    d31a60 = d31a60.add(porcion);
                } else if (dias <= 90) {
                    d61a90 = d61a90.add(porcion);
                } else {
                    mas90d = mas90d.add(porcion);
                }
            }

            // El redondeo puede dejar centavos sin asignar: se van al tramo vigente
            BigDecimal asignado = d0a30.add(d31a60).add(d61a90).add(mas90d);
            d0a30 = d0a30.add(saldoCliente.subtract(asignado));

            saldoTotal = saldoTotal.add(saldoCliente);
            mas30 = mas30.add(d31a60).add(d61a90).add(mas90d);
            mas90 = mas90.add(mas90d);

            clientes.add(ClienteDeudaDTO.builder()
                    .clienteId(cliente.getId())
                    .document(cliente.getDocument())
                    .nombre(cliente.getFullName())
                    .creditLimit(cliente.getCreditLimit())
                    .saldoTotal(saldoCliente)
                    .disponible(cliente.getCreditLimit().subtract(saldoCliente))
                    .antiguedad(Antiguedad.builder()
                            .d0a30(redondear(d0a30))
                            .d31a60(redondear(d31a60))
                            .d61a90(redondear(d61a90))
                            .mas90(redondear(mas90d))
                            .build())
                    .ultimaVenta(ventaDAO
                            .findByClienteIdAndTypeAndStatusOrderBySaleDateDesc(
                                    cliente.getId(), TipoVenta.CREDITO, EstadoVenta.PAGADA)
                            .stream()
                            .map(Venta::getSaleDate)
                            .map(dt -> dt.toLocalDate())
                            .findFirst()
                            .orElse(null))
                    .ultimoPago(abonoDAO.findByClienteIdAndAnnulledAtIsNull(cliente.getId())
                            .stream()
                            .map(Abono::getCreatedAt)
                            .map(dt -> dt.toLocalDate())
                            .max(LocalDate::compareTo)
                            .orElse(null))
                    .build());
        }

        return CuentasPorCobrarResponseDTO.builder()
                .clientes(clientes)
                .resumen(ResumenCobranza.builder()
                        .clientesConDeuda(clientes.size())
                        .saldoTotal(redondear(saldoTotal))
                        .deudaMas30Dias(redondear(mas30))
                        .deudaMas90Dias(redondear(mas90))
                        .build())
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  VENTAS POR HORA Y DEL DIA
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public VentasPorHoraResponseDTO ventasPorHora(LocalDate desde, LocalDate hasta) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hasta.atTime(LocalTime.MAX);

        List<VentaHoraDTO> porHora = new ArrayList<>();
        BigDecimal ingresosTotales = CERO;
        Integer horaPico = null;
        Integer horaBaja = null;
        BigDecimal max = null;
        BigDecimal min = null;

        for (Object[] fila : reporteDAO.ventasPorHora(d, h)) {
            int hora = toLong(fila[0]).intValue();
            long ventas = toLong(fila[1]);
            BigDecimal ingresos = toDecimal(fila[2]);
            BigDecimal costo = toDecimal(fila[3]);

            ingresosTotales = ingresosTotales.add(ingresos);

            porHora.add(VentaHoraDTO.builder()
                    .hora(hora)
                    .ventasCount(ventas)
                    .ingresos(redondear(ingresos))
                    .costo(redondear(costo))
                    .utilidad(redondear(ingresos.subtract(costo)))
                    .ticketPromedio(ventas == 0 ? null
                            : redondear(ingresos.divide(BigDecimal.valueOf(ventas), 2, RoundingMode.HALF_UP)))
                    .build());

            if (max == null || ingresos.compareTo(max) > 0) {
                max = ingresos;
                horaPico = hora;
            }
            if (min == null || ingresos.compareTo(min) < 0) {
                min = ingresos;
                horaBaja = hora;
            }
        }

        return VentasPorHoraResponseDTO.builder()
                .porHora(porHora)
                .horaPico(horaPico)
                .horaBaja(horaBaja)
                .ingresosTotales(redondear(ingresosTotales))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VentasDelDiaResponseDTO ventasDelDia(LocalDate fecha) {
        LocalDateTime d = fecha.atStartOfDay();
        LocalDateTime h = fecha.atTime(LocalTime.MAX);

        BigDecimal ingresos = CERO;
        BigDecimal costo = CERO;
        long unidades = 0;
        long ventasCount = 0;
        BigDecimal credito = CERO;

        for (Object[] fila : reporteDAO.gananciasPorDia(d, h)) {
            ingresos = ingresos.add(toDecimal(fila[3]));
            costo = costo.add(toDecimal(fila[4]));
            ventasCount += toLong(fila[1]);
        }

        unidades = unidadesVendidas(d, h);

        List<Venta> ventas = ventaDAO.buscar(EstadoVenta.PAGADA, null, null, d, h,
                        org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();

        Map<TipoVenta, long[]> conteo = new LinkedHashMap<>();
        Map<TipoVenta, BigDecimal> montos = new LinkedHashMap<>();
        for (Venta v : ventas) {
            conteo.merge(v.getType(), new long[]{1}, (a, b) -> new long[]{a[0] + b[0]});
            montos.merge(v.getType(), v.getTotal(), BigDecimal::add);
            if (v.getType() == TipoVenta.CREDITO) {
                credito = credito.add(v.getTotal());
            }
        }

        List<GananciaTipoVentaDTO> porTipo = new ArrayList<>();
        for (Map.Entry<TipoVenta, long[]> e : conteo.entrySet()) {
            BigDecimal total = montos.get(e.getKey());
            porTipo.add(GananciaTipoVentaDTO.builder()
                    .type(e.getKey())
                    .ventasCount(e.getValue()[0])
                    .total(redondear(total))
                    .porcentaje(porcentaje(total, ingresos))
                    .build());
        }

        List<AbonoDelDiaDTO> abonos = new ArrayList<>();
        BigDecimal recaudado = CERO;
        for (Abono a : abonoDAO.findByCreatedAtBetweenOrderByCreatedAtAsc(d, h)) {
            if (a.isAnulado()) {
                continue;
            }
            recaudado = recaudado.add(a.getAmount());
            abonos.add(AbonoDelDiaDTO.builder()
                    .abonoId(a.getId())
                    .cliente(a.getCliente().getFullName())
                    .amount(a.getAmount())
                    .method(a.getMethod())
                    .build());
        }

        // Top productos del dia
        List<ProductoGananciaDTO> porProducto =
                reporteDAO.gananciasPorProducto(d, h, null).stream()
                        .limit(10)
                        .map(fila -> ProductoGananciaDTO.builder()
                                .productoId(toLong(fila[0]))
                                .sku((String) fila[1])
                                .nombre((String) fila[2])
                                .categoria((String) fila[3])
                                .unidadesVendidas(toLong(fila[4]))
                                .ingresos(redondear(toDecimal(fila[5])))
                                .costo(redondear(toDecimal(fila[6])))
                                .utilidad(redondear(
                                        toDecimal(fila[5]).subtract(toDecimal(fila[6])).subtract(toDecimal(fila[7]))))
                                .build())
                        .toList();

        List<TopProductoDTO> top = porProducto.stream()
                .map(p -> TopProductoDTO.builder()
                        .productoId(p.getProductoId())
                        .nombre(p.getNombre())
                        .unidades(p.getUnidadesVendidas())
                        .ingresos(p.getIngresos())
                        .utilidad(p.getUtilidad())
                        .build())
                .toList();

        return VentasDelDiaResponseDTO.builder()
                .fecha(fecha.toString())
                .resumen(ResumenDia.builder()
                        .ventasCount(ventasCount)
                        .unidadesVendidas(unidades)
                        .ingresosTotal(redondear(ingresos))
                        .costoTotal(redondear(costo))
                        .utilidad(redondear(ingresos.subtract(costo)))
                        .creditoOtorgado(redondear(credito))
                        .abonosRecibidos(redondear(recaudado))
                        .build())
                .porTipoVenta(porTipo)
                .abonosDelDia(abonos)
                .topProductos(top)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private long unidadesVendidas(LocalDateTime desde, LocalDateTime hasta) {
        long total = 0;
        for (Object[] fila : reporteDAO.gananciasPorProducto(desde, hasta, null)) {
            total += toLong(fila[4]);
        }
        return total;
    }

    private BigDecimal redondear(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Parte proporcional: cuanto de `saldo` corresponde a `parte` sobre `total`. */
    private BigDecimal porcentajeDe(BigDecimal saldo, BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(CERO) == 0) {
            return CERO;
        }
        return parte.divide(total, 8, RoundingMode.HALF_UP).multiply(saldo);
    }

    /** Margen porcentual. Si el ingreso es 0, el margen es 0 (regla R-R-05). */
    private BigDecimal margen(BigDecimal utilidad, BigDecimal ingresos) {
        if (ingresos == null || ingresos.compareTo(CERO) == 0) {
            return CERO.setScale(2, RoundingMode.HALF_UP);
        }
        return utilidad.multiply(CIEN).divide(ingresos, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(CERO) == 0) {
            return CERO.setScale(2, RoundingMode.HALF_UP);
        }
        return parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    private Long toLong(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private BigDecimal toDecimal(Object valor) {
        if (valor == null) {
            return CERO;
        }
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return CERO;
    }

    private LocalDate toLocalDate(Object valor) {
        if (valor instanceof LocalDate ld) {
            return ld;
        }
        if (valor instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        }
        if (valor instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private record AcumuladoDia(
            LocalDate fecha,
            long ventasCount,
            long unidades,
            BigDecimal ingresos,
            BigDecimal costo,
            BigDecimal mermas) {
    }
}