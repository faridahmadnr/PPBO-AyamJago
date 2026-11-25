const TILE_SIZE = 34;
const MAZE_WIDTH = 25;
const MAZE_HEIGHT = 20;
const HUD_HEIGHT = 130;
const CANVAS_WIDTH = MAZE_WIDTH * TILE_SIZE;
const CANVAS_HEIGHT = MAZE_HEIGHT * TILE_SIZE + HUD_HEIGHT;
const FPS = 60;
const FRAME_MS = 1000 / FPS;

const KILLS_PER_PLAYER_LEVEL = 5;
const DEATHS_PER_ENEMY_LEVEL = 5;
const BASE_ENEMY_HEALTH = 2;
const MOVE_SPEED = 0.1;

const DIR_UP = 0;
const DIR_RIGHT = 1;
const DIR_DOWN = 2;
const DIR_LEFT = 3;

const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");
const restartBtn = document.getElementById("restartBtn");

function loadImage(src) {
  const img = new Image();
  img.src = src;
  return img;
}

const assets = {
  wall: loadImage("assets/wall.png"),
  floor: loadImage("assets/floor.jpeg"),
  playerIdle: [loadImage("assets/player-idle1.png"), loadImage("assets/player-idle2.png")],
  playerMove: [loadImage("assets/player-move1.png"), loadImage("assets/player-move2.png")],
  enemyIdle: [loadImage("assets/enemy-idle1.png"), loadImage("assets/enemy-idle2.png")],
  enemyMove: [loadImage("assets/enemy-move1.png"), loadImage("assets/enemy-move2.png")],
  corn: [loadImage("assets/corn.png"), loadImage("assets/corn2.png")],
};

function drawSprite(img, x, y, size, flipRight = false) {
  if (!img || !img.complete || img.naturalWidth === 0) return false;
  ctx.save();
  if (flipRight) {
    ctx.translate(x + size, y);
    ctx.scale(-1, 1);
    ctx.drawImage(img, 0, 0, size, size);
  } else {
    ctx.drawImage(img, x, y, size, size);
  }
  ctx.restore();
  return true;
}

class Rooster {
  constructor(row, col, health, defaultDirection, idleFrames, moveFrames, fallbackColor) {
    this.row = row;
    this.col = col;
    this.visualRow = row;
    this.visualCol = col;
    this.health = health;
    this.isMoving = false;
    this.moveCount = 0;
    this.direction = defaultDirection;
    this.idleFrames = idleFrames;
    this.moveFrames = moveFrames;
    this.fallbackColor = fallbackColor;
    this.animCounter = 0;
  }

  update() {
    const dx = this.col - this.visualCol;
    const dy = this.row - this.visualRow;

    if (Math.abs(dx) < MOVE_SPEED && Math.abs(dy) < MOVE_SPEED) {
      this.visualRow = this.row;
      this.visualCol = this.col;
      this.isMoving = false;
    } else {
      if (Math.abs(dy) >= MOVE_SPEED) this.visualRow += Math.sign(dy) * MOVE_SPEED;
      if (Math.abs(dx) >= MOVE_SPEED) this.visualCol += Math.sign(dx) * MOVE_SPEED;
      this.isMoving = true;
    }

    this.animCounter++;
  }

  draw() {
    const px = Math.round(this.visualCol * TILE_SIZE);
    const py = Math.round(this.visualRow * TILE_SIZE);
    const frames = this.isMoving ? this.moveFrames : this.idleFrames;
    const frameIndex = Math.floor(this.animCounter / 30) % frames.length;
    const flipRight = this.direction === DIR_RIGHT;

    if (!drawSprite(frames[frameIndex], px, py, TILE_SIZE, flipRight)) {
      ctx.fillStyle = this.fallbackColor;
      ctx.fillRect(px, py, TILE_SIZE, TILE_SIZE);
    }

    ctx.fillStyle = "#ffffff";
    ctx.font = "bold 12px Arial, sans-serif";
    ctx.textAlign = "left";
    ctx.fillText(`♥${this.health}`, px + 5, py - 4);
  }
}

let maze = [];
let player;
let enemy;
let corn = null;
let playerTurn = true;
let turnCounter = 0;
let playerLevel = 1;
let playerKillCount = 0;
let totalKills = 0;
let enemyDeathCount = 0;
let enemyLevel = 1;
let survivalTime = 0;
let gameOver = false;
let eventLog = "Your turn: 2 moves per turn. Walk into the enemy rooster to attack!";

function createMaze() {
  maze = [];
  for (let r = 0; r < MAZE_HEIGHT; r++) {
    const row = [];
    for (let c = 0; c < MAZE_WIDTH; c++) {
      let isWall = c === 0 || r === 0 || c === MAZE_WIDTH - 1 || r === MAZE_HEIGHT - 1;
      if (!isWall && c % 3 === 0 && r % 2 === 0) {
        isWall = true;
      }
      row.push(isWall);
    }
    maze.push(row);
  }

  maze[1][1] = false;
  maze[1][2] = false;
  maze[2][1] = false;
  maze[MAZE_HEIGHT - 2][MAZE_WIDTH - 2] = false;
}

function spawnCorn() {
  let r, c;
  let tries = 0;
  do {
    r = Math.floor(Math.random() * MAZE_HEIGHT);
    c = Math.floor(Math.random() * MAZE_WIDTH);
    tries++;
  } while (
    (maze[r][c] ||
      (r === player.row && c === player.col) ||
      (r === enemy.row && c === enemy.col)) &&
    tries < 200
  );

  if (!maze[r][c]) {
    corn = { row: r, col: c, animCounter: 0 };
    eventLog = "A golden corn spawned in the maze! (+3 HP)";
  }
}

function initializeGame() {
  createMaze();
  player = new Rooster(1, 1, 5, DIR_RIGHT, assets.playerIdle, assets.playerMove, "#ef4444");
  enemy = new Rooster(
    MAZE_HEIGHT - 2,
    MAZE_WIDTH - 2,
    BASE_ENEMY_HEALTH,
    DIR_LEFT,
    assets.enemyIdle,
    assets.enemyMove,
    "#22c55e"
  );
  corn = null;
  playerTurn = true;
  turnCounter = 0;
  playerLevel = 1;
  playerKillCount = 0;
  totalKills = 0;
  enemyDeathCount = 0;
  enemyLevel = 1;
  survivalTime = 0;
  gameOver = false;
  eventLog = "Your turn: 2 moves per turn. Walk into the enemy rooster to attack!";
}

function respawnEnemy() {
  const enemyHealthBonus = Math.floor(enemyDeathCount / DEATHS_PER_ENEMY_LEVEL);
  const newHealth = BASE_ENEMY_HEALTH + enemyHealthBonus;

  let spawnRow = MAZE_HEIGHT - 2;
  let spawnCol = MAZE_WIDTH - 2;

  if (player.row === spawnRow && player.col === spawnCol) {
    spawnRow = 1;
    spawnCol = 1;
    if (player.row === spawnRow && player.col === spawnCol) {
      do {
        spawnRow = Math.floor(Math.random() * (MAZE_HEIGHT - 2)) + 1;
        spawnCol = Math.floor(Math.random() * (MAZE_WIDTH - 2)) + 1;
      } while (maze[spawnRow][spawnCol] || (spawnRow === player.row && spawnCol === player.col));
    }
  }

  enemy = new Rooster(
    spawnRow,
    spawnCol,
    newHealth,
    DIR_LEFT,
    assets.enemyIdle,
    assets.enemyMove,
    "#22c55e"
  );
}

function endPlayerMove() {
  if (player.moveCount >= 2) {
    playerTurn = false;
    turnCounter++;
    if (corn === null && turnCounter % 3 === 0 && Math.random() < 0.25) {
      spawnCorn();
    }
  }
}

function handlePlayerAttackCollision() {
  player.health -= 1;
  enemy.health -= 1;
  eventLog = `Clash! Both took 1 dmg (Player HP: ${player.health}, Enemy HP: ${enemy.health})`;

  if (enemy.health <= 0) {
    playerKillCount++;
    totalKills++;
    enemyDeathCount++;
    eventLog = "Enemy defeated! Respawning stronger challenger...";

    if (playerKillCount >= KILLS_PER_PLAYER_LEVEL) {
      playerLevel++;
      playerKillCount = 0;
      player.health += 3;
      eventLog = `PLAYER LEVEL UP! Level ${playerLevel} (+3 HP)`;
    }

    if (enemyDeathCount % DEATHS_PER_ENEMY_LEVEL === 0) {
      enemyLevel++;
    }

    respawnEnemy();
  }

  if (player.health <= 0) {
    gameOver = true;
  }
}

function handleKeyPress(key) {
  if (gameOver || !playerTurn || player.isMoving) return;

  let dRow = 0;
  let dCol = 0;
  if (key === "ArrowUp" || key === "w" || key === "W") dRow = -1;
  else if (key === "ArrowDown" || key === "s" || key === "S") dRow = 1;
  else if (key === "ArrowLeft" || key === "a" || key === "A") dCol = -1;
  else if (key === "ArrowRight" || key === "d" || key === "D") dCol = 1;
  else return;

  if (dRow < 0) player.direction = DIR_UP;
  else if (dRow > 0) player.direction = DIR_DOWN;
  else if (dCol < 0) player.direction = DIR_LEFT;
  else if (dCol > 0) player.direction = DIR_RIGHT;

  const targetRow = player.row + dRow;
  const targetCol = player.col + dCol;

  if (targetRow === enemy.row && targetCol === enemy.col) {
    handlePlayerAttackCollision();
    player.moveCount++;
    if (!gameOver) endPlayerMove();
    return;
  }

  if (
    targetRow >= 0 &&
    targetRow < MAZE_HEIGHT &&
    targetCol >= 0 &&
    targetCol < MAZE_WIDTH &&
    !maze[targetRow][targetCol]
  ) {
    player.row = targetRow;
    player.col = targetCol;
    player.isMoving = true;
    player.moveCount++;

    if (corn && player.row === corn.row && player.col === corn.col) {
      player.health += 3;
      corn = null;
      eventLog = `Collected corn! Health +3 (HP: ${player.health})`;
    }

    endPlayerMove();
  }
}

function isValidEnemyMove(r, c) {
  if (r < 0 || r >= MAZE_HEIGHT || c < 0 || c >= MAZE_WIDTH) return false;
  if (r === player.row && c === player.col) return false;
  return !maze[r][c];
}

function tryEnemyMove(newRow, newCol, dRow, dCol) {
  if (!isValidEnemyMove(newRow, newCol)) return false;
  enemy.row = newRow;
  enemy.col = newCol;
  enemy.isMoving = true;
  if (dRow < 0) enemy.direction = DIR_UP;
  else if (dRow > 0) enemy.direction = DIR_DOWN;
  else if (dCol < 0) enemy.direction = DIR_LEFT;
  else if (dCol > 0) enemy.direction = DIR_RIGHT;
  return true;
}

function processEnemyTurn() {
  if (enemy.isMoving || gameOver) return;

  const rowDiff = Math.abs(player.row - enemy.row);
  const colDiff = Math.abs(player.col - enemy.col);
  let dRow = 0;
  let dCol = 0;

  if (rowDiff > colDiff) {
    dRow = player.row < enemy.row ? -1 : 1;
  } else if (colDiff > rowDiff) {
    dCol = player.col < enemy.col ? -1 : 1;
  } else if (rowDiff > 0 || colDiff > 0) {
    if (Math.random() < 0.5) {
      dRow = player.row < enemy.row ? -1 : 1;
    } else {
      dCol = player.col < enemy.col ? -1 : 1;
    }
  }

  const targetRow = enemy.row + dRow;
  const targetCol = enemy.col + dCol;
  const adjH = enemy.row === player.row && Math.abs(enemy.col - player.col) === 1;
  const adjV = enemy.col === player.col && Math.abs(enemy.row - player.row) === 1;

  if (targetRow === player.row && targetCol === player.col && (adjH || adjV)) {
    player.health -= 1;
    eventLog = `Enemy attacked you! (-1 HP, remaining: ${player.health})`;
    if (player.health <= 0) {
      gameOver = true;
      return;
    }
  } else {
    const stepRow = player.row > enemy.row ? 1 : player.row < enemy.row ? -1 : 0;
    const stepCol = player.col > enemy.col ? 1 : player.col < enemy.col ? -1 : 0;
    let moved = false;

    if (rowDiff > colDiff) {
      moved = tryEnemyMove(enemy.row + stepRow, enemy.col, stepRow, 0);
      if (!moved && stepCol !== 0) {
        moved = tryEnemyMove(enemy.row, enemy.col + stepCol, 0, stepCol);
      }
    } else {
      moved = tryEnemyMove(enemy.row, enemy.col + stepCol, 0, stepCol);
      if (!moved && stepRow !== 0) {
        moved = tryEnemyMove(enemy.row + stepRow, enemy.col, stepRow, 0);
      }
    }

    if (!moved) {
      const dirs = [
        [0, 1],
        [1, 0],
        [0, -1],
        [-1, 0],
      ];
      for (const [dr, dc] of dirs) {
        if (tryEnemyMove(enemy.row + dr, enemy.col + dc, dr, dc)) break;
      }
    }
  }

  survivalTime++;
  player.moveCount = 0;
  playerTurn = true;
}

function update() {
  if (gameOver) return;
  player.update();
  enemy.update();
  if (corn) corn.animCounter++;

  if (!player.isMoving && !enemy.isMoving && !playerTurn) {
    processEnemyTurn();
  }
}

function draw() {
  ctx.fillStyle = "#000000";
  ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

  if (gameOver) {
    ctx.textAlign = "center";
    ctx.fillStyle = "#ef4444";
    ctx.font = "bold 52px Arial, sans-serif";
    ctx.fillText("GAME OVER", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 - 40);

    ctx.fillStyle = "#facc15";
    ctx.font = "bold 24px Arial, sans-serif";
    ctx.fillText(
      `Level: ${playerLevel}   |   Kills: ${totalKills}   |   Survived: ${survivalTime} turns`,
      CANVAS_WIDTH / 2,
      CANVAS_HEIGHT / 2 + 15
    );

    ctx.fillStyle = "#ffffff";
    ctx.font = "20px Arial, sans-serif";
    ctx.fillText("Press R or Click Restart to Play Again", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 65);
    return;
  }

  for (let r = 0; r < MAZE_HEIGHT; r++) {
    for (let c = 0; c < MAZE_WIDTH; c++) {
      const x = c * TILE_SIZE;
      const y = r * TILE_SIZE;
      const img = maze[r][c] ? assets.wall : assets.floor;
      if (!drawSprite(img, x, y, TILE_SIZE, false)) {
        ctx.fillStyle = maze[r][c] ? "#334155" : "#e2e8f0";
        ctx.fillRect(x, y, TILE_SIZE, TILE_SIZE);
      }
    }
  }

  if (corn) {
    const cx = corn.col * TILE_SIZE;
    const cy = corn.row * TILE_SIZE;
    const cornFrame = Math.floor(corn.animCounter / 45) % assets.corn.length;
    if (!drawSprite(assets.corn[cornFrame], cx, cy, TILE_SIZE, false)) {
      ctx.fillStyle = "#facc15";
      ctx.beginPath();
      ctx.arc(cx + TILE_SIZE / 2, cy + TILE_SIZE / 2, TILE_SIZE / 3, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  enemy.draw();
  player.draw();

  const uiY = MAZE_HEIGHT * TILE_SIZE + 14;
  ctx.textAlign = "left";
  ctx.font = "bold 14px Arial, sans-serif";
  ctx.fillStyle = playerTurn ? "#4ade80" : "#f87171";
  ctx.fillText(
    playerTurn ? `Player Turn: ${2 - player.moveCount} moves left` : "Enemy Turn...",
    14,
    uiY + 12
  );

  ctx.fillStyle = "#ffffff";
  ctx.fillText(`Player Health: ${player.health}`, 14, uiY + 38);
  ctx.fillText(`Enemy Health: ${enemy.health}`, 240, uiY + 38);

  ctx.fillStyle = "#facc15";
  ctx.fillText(`Player Level: ${playerLevel}`, 14, uiY + 62);
  ctx.fillText(`Kills: ${playerKillCount}/${KILLS_PER_PLAYER_LEVEL} (Total: ${totalKills})`, 14, uiY + 84);

  ctx.fillStyle = "#f87171";
  ctx.fillText(`Enemy Deaths: ${enemyDeathCount}`, 240, uiY + 62);
  ctx.fillText(
    `Enemy Health Bonus: +${Math.floor(enemyDeathCount / DEATHS_PER_ENEMY_LEVEL)}`,
    240,
    uiY + 84
  );

  ctx.fillStyle = "#22d3ee";
  ctx.fillText(`Survival Time: ${survivalTime} turns`, 14, uiY + 108);

  ctx.fillStyle = "#94a3b8";
  ctx.font = "13px Arial, sans-serif";
  ctx.fillText(eventLog, 240, uiY + 108);
}

window.addEventListener("keydown", (e) => {
  if ([" ", "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(e.key)) {
    e.preventDefault();
  }
  if (e.key === "r" || e.key === "R") {
    initializeGame();
    return;
  }
  handleKeyPress(e.key);
});

restartBtn.addEventListener("click", () => {
  initializeGame();
  canvas.focus();
});

canvas.addEventListener("click", () => {
  canvas.focus();
  if (gameOver) initializeGame();
});

initializeGame();

let lastTime = performance.now();
let delta = 0;

function gameLoop(now) {
  delta += (now - lastTime) / FRAME_MS;
  lastTime = now;
  if (delta > 5) delta = 5;

  while (delta >= 1) {
    update();
    delta--;
  }

  draw();
  requestAnimationFrame(gameLoop);
}

requestAnimationFrame(gameLoop);
