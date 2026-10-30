package com.scooter.control.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

const val BLE_TIMEOUT = "BLE: handshake timeout"

enum class LockState { OPEN, LOCKED }

enum class BleStatus { IDLE, SCANNING, CONNECTING, CONNECTED, FAILED }

/**
 * Вымышленное фейковое устройство для демо-сканирования BLE.
 * Бренд выдуманный ("LastBot IOT" / "WSH-xxxx"), ни одно название или UUID
 * не привязаны к реальному производителю или реальному самокату.
 */
data class FakeDevice(
    val name: String,
    val uuid: String,
    val rssi: Int,
)

data class ScooterState(
    val model: String = "LastBot by excellent",
    val battery: Int = 76,
    val speedKmh: Double = 0.0,
    val rangeKm: Double = 21.7,
    val lock: LockState = LockState.LOCKED,
    val headlightOn: Boolean = false,
    val alarmOn: Boolean = false,
    val cruiseOn: Boolean = false,
    val isBusy: Boolean = false,
    val ble: BleStatus = BleStatus.IDLE,
    val bleDevice: String? = null,
    val lastAction: String? = null,
    val lastIsError: Boolean = false,
    /** Растёт при каждом ответе — чтобы повторно запускать анимацию. */
    val eventId: Int = 0,
)

private fun randomUuid(): String {
    fun hex(n: Int) = (1..n).joinToString("") { Random.nextInt(16).toString(16) }
    return "${hex(8)}-${hex(4)}-${hex(4)}-${hex(4)}-${hex(12)}"
}

private fun randomDeviceList(count: Int = 4): List<FakeDevice> =
    (1..count).map {
        val wsh = (1000..9999).random()
        FakeDevice(
            name = "LastBot IOT · WSH-$wsh",
            uuid = randomUuid(),
            rssi = -(40..90).random(),
        )
    }

/**
 * ДЕМО-РЕЖИМ (это симулятор, не реальный BLE-стек): устройства, их UUID,
 * заряд и скорость — случайные числа, сгенерированные на телефоне.
 * Никакого реального подключения к реальным самокатам нет и не предполагается.
 */
class ScooterRepository {

    private val _state = MutableStateFlow(ScooterState())
    val state: StateFlow<ScooterState> = _state

    private val _devices = MutableStateFlow<List<FakeDevice>>(emptyList())
    val devices: StateFlow<List<FakeDevice>> = _devices

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Живое обновление показателей — лёгкий случайный дрейф значений.
        scope.launch {
            while (true) {
                delay(100)
                _state.update {
                    if (it.ble != BleStatus.CONNECTED) return@update it
                    val speed = (it.speedKmh + Random.nextDouble(-0.6, 0.6)).coerceIn(0.0, 27.0)
                    val battery = (it.battery + if (Random.nextInt(40) == 0) -1 else 0).coerceIn(1, 100)
                    val range = (it.rangeKm + Random.nextDouble(-0.05, 0.02)).coerceIn(0.0, 40.0)
                    it.copy(
                        speedKmh = Math.round(speed * 10) / 10.0,
                        battery = battery,
                        rangeKm = Math.round(range * 10) / 10.0,
                    )
                }
            }
        }
    }

    suspend fun startScan() {
        _state.update { it.copy(ble = BleStatus.SCANNING, bleDevice = null, lastAction = null) }
        _devices.value = emptyList()
        delay(900)
        _devices.value = randomDeviceList()
        _state.update { it.copy(ble = BleStatus.IDLE) }
    }

    suspend fun connectTo(device: FakeDevice) {
        _state.update { it.copy(ble = BleStatus.CONNECTING, bleDevice = device.name) }
        delay(1100)
        // Демо: подключение «успешное» в 75% случаев, иначе — таймаут хендшейка.
        if (Random.nextInt(4) != 0) {
            _state.update {
                it.copy(
                    ble = BleStatus.CONNECTED,
                    model = device.name,
                    battery = Random.nextInt(15, 100),
                    speedKmh = 0.0,
                    rangeKm = Random.nextDouble(3.0, 35.0).let { v -> Math.round(v * 10) / 10.0 },
                    lastAction = "Подключено к ${device.name}",
                    lastIsError = false,
                    eventId = it.eventId + 1,
                )
            }
        } else {
            _state.update {
                it.copy(
                    ble = BleStatus.FAILED,
                    lastAction = BLE_TIMEOUT,
                    lastIsError = true,
                    eventId = it.eventId + 1,
                )
            }
        }
    }

    suspend fun unlock() = act { it.copy(lock = LockState.OPEN, lastAction = "Замок открыт") }
    suspend fun lock() = act { it.copy(lock = LockState.LOCKED, lastAction = "Замок закрыт") }
    suspend fun find() = act { it.copy(lastAction = "Самокат подаёт сигнал") }
    suspend fun horn() = act { it.copy(lastAction = "Гудок") }
    suspend fun toggleAlarm() = act { it.copy(alarmOn = !it.alarmOn, lastAction = if (!it.alarmOn) "Сигнализация включена" else "Сигнализация выключена") }
    suspend fun toggleCruise() = act { it.copy(cruiseOn = !it.cruiseOn, lastAction = if (!it.cruiseOn) "Круиз-контроль включён" else "Круиз-контроль выключен") }
    suspend fun toggleHeadlight() = act { it.copy(headlightOn = !it.headlightOn, lastAction = if (!it.headlightOn) "Фонарь включён" else "Фонарь выключен") }

    private suspend fun act(onSuccess: (ScooterState) -> ScooterState) {
        if (_state.value.ble != BleStatus.CONNECTED) {
            _state.update {
                it.copy(lastAction = "Сначала подключитесь к устройству", lastIsError = true, eventId = it.eventId + 1)
            }
            return
        }
        _state.update { it.copy(isBusy = true) }
        delay(450)
        _state.update { onSuccess(it).copy(isBusy = false, lastIsError = false, eventId = it.eventId + 1) }
    }
}
