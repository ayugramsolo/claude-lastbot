"""Telegram-бот демо-приложения LastBot (симулятор, реальным BLE не управляет).

Поток: /start -> бот сразу выдаёт одноразовый ключ (токен) и ссылку
scooterapp://auth?token=...&name=...&username=...&uid=...&server=...
Входа по паролю нет: ключ для входа в демо-приложение может получить любой,
кто напишет боту /start. Это открытый демо-доступ, а не защищённая авторизация.
Telegram не даёт делать кнопки с кастомной схемой, поэтому кнопка ведёт на
https-редиректор (его поднимает этот же процесс), а тот перекидывает на scooterapp://.

Профиль (имя, @username, ID, аватарка) берётся из Telegram и отдаётся приложению:
  GET /api/profile?token=...  -> JSON
  GET /api/avatar?token=...   -> JPEG

Переменные окружения:
  BOT_TOKEN          токен от @BotFather (обязательно)
  PUBLIC_BASE_URL    https-адрес VPS, например https://myscooter.mooo.com
                     (нужен для кнопки и загрузки профиля в приложение)
  PORT               порт веб-части (по умолчанию 8080)
"""
import html
import json
import logging
import os
import secrets
from urllib.parse import urlencode

from aiohttp import web
from telegram import InlineKeyboardButton, InlineKeyboardMarkup, Update
from telegram.ext import Application, CommandHandler, ContextTypes

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("lastbot")

BOT_TOKEN = os.environ["BOT_TOKEN"]
PUBLIC_BASE_URL = os.environ.get("PUBLIC_BASE_URL", "").rstrip("/")
PORT = int(os.environ.get("PORT", "8080"))

# Для продакшена замени на Redis/БД.
_issued_tokens: dict[str, int] = {}          # token -> user_id
_profiles: dict[int, dict] = {}              # user_id -> профиль из Telegram


def build_deep_link(token: str, prof: dict) -> str:
    params = {
        "token": token,
        "name": prof["name"],
        "username": prof["username"],
        "uid": str(prof["id"]),
    }
    if PUBLIC_BASE_URL:
        params["server"] = PUBLIC_BASE_URL
    return "scooterapp://auth?" + urlencode(params)


async def _load_profile(context: ContextTypes.DEFAULT_TYPE, user) -> dict:
    avatar = None
    try:
        photos = await context.bot.get_user_profile_photos(user.id, limit=1)
        if photos.total_count > 0:
            file = await context.bot.get_file(photos.photos[0][-1].file_id)
            avatar = bytes(await file.download_as_bytearray())
    except Exception as e:  # аватарки может не быть или быть скрыта
        log.warning("avatar load failed: %s", e)
    return {
        "id": user.id,
        "name": user.full_name or "",
        "username": user.username or "",
        "language": user.language_code or "",
        "avatar": avatar,
    }


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    # Открытый демо-доступ: ключ для входа в симулятор выдаётся сразу любому,
    # кто напишет /start, без пароля и без проверки прав.
    user = update.effective_user
    chat_id = update.effective_chat.id

    prof = await _load_profile(context, user)
    _profiles[user.id] = prof
    token = secrets.token_urlsafe(32)
    _issued_tokens[token] = user.id

    if PUBLIC_BASE_URL:
        markup = InlineKeyboardMarkup(
            [[InlineKeyboardButton("Войти в LastBot (демо)", url=f"{PUBLIC_BASE_URL}/open?token={token}")]]
        )
        await context.bot.send_message(
            chat_id,
            f"Привет, {prof['name']}! Это демо-симулятор LastBot. Нажми кнопку, чтобы войти:",
            reply_markup=markup,
        )
    else:
        await context.bot.send_message(
            chat_id,
            "Это демо-симулятор LastBot. Открой ссылку на телефоне с приложением:\n"
            + build_deep_link(token, prof),
        )


def _profile_for(request: web.Request):
    user_id = _issued_tokens.get(request.query.get("token", ""))
    return _profiles.get(user_id) if user_id else None


async def redirect(request: web.Request) -> web.Response:
    token = request.query.get("token", "")
    prof = _profile_for(request)
    if not prof:
        return web.Response(status=404, text="Ссылка недействительна")
    link = build_deep_link(token, prof)
    page = (
        "<!doctype html><meta charset=utf-8>"
        f'<meta http-equiv="refresh" content="0;url={html.escape(link)}">'
        f'<p><a href="{html.escape(link)}">Открыть LastBot</a></p>'
        f"<script>location.href={json.dumps(link)}</script>"
    )
    return web.Response(text=page, content_type="text/html")


async def api_profile(request: web.Request) -> web.Response:
    prof = _profile_for(request)
    if not prof:
        return web.json_response({"error": "unauthorized"}, status=401)
    return web.json_response(
        {
            "id": prof["id"],
            "name": prof["name"],
            "username": prof["username"],
            "language": prof["language"],
            "has_avatar": prof["avatar"] is not None,
        }
    )


async def api_avatar(request: web.Request) -> web.Response:
    prof = _profile_for(request)
    if not prof or not prof["avatar"]:
        return web.Response(status=404)
    return web.Response(body=prof["avatar"], content_type="image/jpeg")


async def post_init(app: Application) -> None:
    if not PUBLIC_BASE_URL:
        return
    web_app = web.Application()
    web_app.add_routes(
        [
            web.get("/open", redirect),
            web.get("/api/profile", api_profile),
            web.get("/api/avatar", api_avatar),
        ]
    )
    runner = web.AppRunner(web_app)
    await runner.setup()
    await web.TCPSite(runner, "0.0.0.0", PORT).start()
    log.info("Web server on :%s", PORT)


def main() -> None:
    app = Application.builder().token(BOT_TOKEN).post_init(post_init).build()
    app.add_handler(CommandHandler("start", start))
    app.run_polling()


if __name__ == "__main__":
    main()
