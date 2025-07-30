using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CoworkingApp.Data;
using CoworkingApp.Models;
using System.Security.Claims;
using System.Threading.Tasks;
using System.Linq;

namespace CoworkingApp.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize] // Tous les endpoints nécessitent authentification
    public class ReservationsController : ControllerBase
    {
        private readonly ApplicationDbContext _context;

        public ReservationsController(ApplicationDbContext context)
        {
            _context = context;
        }

        // CLIENT ET ADMIN: voir les réservations de l'utilisateur connecté
        [HttpGet("me")]
        [Authorize(Roles = "Client,Admin")]
        public async Task<IActionResult> GetMyReservations()
        {
            var userIdString = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (!int.TryParse(userIdString, out int userId))
                return Unauthorized();

            var reservations = await _context.Reservations
                .Include(r => r.Creneau)
                .Where(r => r.UserId == userId)
                .ToListAsync();

            return Ok(reservations);
        }

        // ADMIN: voir toutes les réservations
        [HttpGet]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> GetAllReservations()
        {
            var reservations = await _context.Reservations
                .Include(r => r.Creneau)
                .Include(r => r.User)
                .ToListAsync();

            return Ok(reservations);
        }

        // CLIENT: créer une réservation
        [HttpPost]
        [Authorize(Roles = "Client")]
        public async Task<IActionResult> CreateReservation([FromBody] int creneauId)
        {
            var userIdString = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (!int.TryParse(userIdString, out int userId))
                return Unauthorized();

            var creneau = await _context.Creneaux.FindAsync(creneauId);
            if (creneau == null || !creneau.Disponible)
                return BadRequest("Le créneau sélectionné n'est pas disponible.");

            var reservation = new Reservation
            {
                UserId = userId,
                CreneauId = creneauId
            };

            creneau.Disponible = false;

            _context.Reservations.Add(reservation);
            _context.Creneaux.Update(creneau);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetMyReservations), new { id = reservation.Id }, reservation);
        }

        // CLIENT: annuler sa réservation
        [HttpDelete("{id}")]
        [Authorize(Roles = "Client")]
        public async Task<IActionResult> CancelReservation(int id)
        {
            var userIdString = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (!int.TryParse(userIdString, out int userId))
                return Unauthorized();

            var reservation = await _context.Reservations
                .Include(r => r.Creneau)
                .FirstOrDefaultAsync(r => r.Id == id && r.UserId == userId);

            if (reservation == null)
                return NotFound();

            reservation.Creneau.Disponible = true;

            _context.Reservations.Remove(reservation);
            _context.Creneaux.Update(reservation.Creneau);
            await _context.SaveChangesAsync();

            return NoContent();
        }

        // CLIENT: modifier sa réservation
        public class ReservationUpdateModel
        {
            public int NewCreneauId { get; set; }
        }

        [HttpPut("{id}")]
        [Authorize(Roles = "Client")]
        public async Task<IActionResult> UpdateReservation(int id, [FromBody] ReservationUpdateModel model)
        {
            var userIdString = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (!int.TryParse(userIdString, out int userId))
                return Unauthorized();

            var reservation = await _context.Reservations
                .Include(r => r.Creneau)
                .FirstOrDefaultAsync(r => r.Id == id && r.UserId == userId);

            if (reservation == null)
                return NotFound();

            var newCreneau = await _context.Creneaux.FindAsync(model.NewCreneauId);
            if (newCreneau == null || !newCreneau.Disponible)
                return BadRequest("Le nouveau créneau n'est pas disponible.");

            // Libérer l'ancien créneau
            reservation.Creneau.Disponible = true;

            // Réserver le nouveau créneau
            newCreneau.Disponible = false;

            // Mise à jour
            reservation.CreneauId = model.NewCreneauId;

            _context.Creneaux.Update(reservation.Creneau);
            _context.Creneaux.Update(newCreneau);
            _context.Reservations.Update(reservation);

            await _context.SaveChangesAsync();

            return NoContent();
        }

        // ADMIN: supprimer une réservation (exemple gestion admin)
        [HttpDelete("admin/{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> DeleteReservationAdmin(int id)
        {
            var reservation = await _context.Reservations
                .Include(r => r.Creneau)
                .FirstOrDefaultAsync(r => r.Id == id);

            if (reservation == null)
                return NotFound();

            // Rendre le créneau dispo à nouveau
            reservation.Creneau.Disponible = true;

            _context.Reservations.Remove(reservation);
            _context.Creneaux.Update(reservation.Creneau);
            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}
