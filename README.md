# Astrax Test Messenger by AI

MVP messenger with Android Kotlin/Jetpack Compose client and Kotlin Ktor backend.

## Structure

```text
Astrax/
  backend/          # Ktor REST + WebSocket server
  android/          # Jetpack Compose client
  deploy/           # VPS deployment scripts (nginx, systemd)
```

## Backend (local dev)

```bash
./gradlew :backend:run
```

Server listens on `http://0.0.0.0:8080`, SQLite data in `astrax.db`.

```bash
export ASTRAX_JWT_SECRET="replace-with-long-random-secret"
```

## Android (local dev)

Emulator default: `http://10.0.2.2:8080` (host machine).

For a physical device on the same Wi-Fi:

```properties
# local.properties
astrax.baseUrl=http://192.168.1.79:8080
```

```bash
./gradlew :android:assembleDebug
```

## Remote deployment (different cities)

To chat from different cities, deploy the backend on a public VPS with HTTPS.

### Minimum server requirements

| Parameter | Value |
|-----------|-------|
| CPU | 1 vCPU |
| RAM | 512 MB – 1 GB |
| Disk | 10 GB SSD |
| OS | Ubuntu 22.04 or 24.04 |
| Users | up to 10 (experimental load) |

Examples: Hetzner CX11 (~€4/mo), Timeweb minimal VPS, DigitalOcean $4 droplet.

No database server needed — SQLite file on disk is enough.

### What to provide for deployment

To connect and deploy myself, I need:

1. **VPS SSH access** — `user@IP` and SSH key or password
2. **Domain** — e.g. `astrax.example.com` with A-record pointing to VPS IP (needed for HTTPS)
3. **JWT secret** — random string 32+ chars (or I generate one)

Optional: if no domain, we can use IP + self-signed cert, but then each phone needs manual cert install — not recommended.

### Deployment steps

**1. On the VPS (once):**

```bash
# Upload deploy/ folder, then:
sudo bash setup-server.sh astrax.example.com

# Create env file:
sudo cp env.example /opt/astrax/.env
sudo nano /opt/astrax/.env   # set ASTRAX_JWT_SECRET and ASTRAX_CORS_HOSTS
sudo chown astrax:astrax /opt/astrax/.env
sudo systemctl start astrax-backend
```

**2. From your machine (each update):**

```bash
cd deploy
./deploy.sh root@YOUR_VPS_IP
```

**3. Android — point to remote server:**

```properties
# local.properties
astrax.baseUrl=https://astrax.example.com
```

```bash
./gradlew :android:assembleRelease
# or assembleDebug for testing
```

Install the APK on phones — users in any city can register and chat.

### Architecture

```text
Phone (City A) ──HTTPS/WSS──┐
                             ├──► nginx (443) ──► Ktor (8080) ──► SQLite
Phone (City B) ──HTTPS/WSS──┘
```

WebSocket path: `wss://your-domain/ws/chats/{id}`

### Environment variables

| Variable | Description | Example |
|----------|-------------|---------|
| `ASTRAX_JWT_SECRET` | JWT signing key (required in prod) | random 32+ chars |
| `ASTRAX_DB_URL` | SQLite path | `jdbc:sqlite:/opt/astrax/data/astrax.db` |
| `ASTRAX_CORS_HOSTS` | Allowed CORS hosts (comma-separated) | `astrax.example.com` |

## Testing

- **Server health:** `curl https://your-domain/health`
- **Android emulator:** I cannot run Android Studio emulator directly. You test on emulator or a real device; I verify the server via SSH and curl.
- **Emulator for local dev:** useful if the server runs on your machine (`10.0.2.2:8080`). For remote VPS testing, a real phone or emulator with `astrax.baseUrl=https://...` works the same.
