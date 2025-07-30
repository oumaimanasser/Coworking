using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CoworkingApp.Models;
using Microsoft.AspNetCore.Authorization;
using CoworkingApp.Data;

[Route("api/[controller]")]
[ApiController]
public class CreneauxController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public CreneauxController(ApplicationDbContext context)
    {
        _context = context;
    }

    // GET api/creneaux — liste des créneaux disponibles
    [HttpGet]
    public async Task<IActionResult> GetDisponibleCreneaux()
    {
        var creneaux = await _context.Creneaux
            .Where(c => c.Disponible)
            .ToListAsync();
        return Ok(creneaux);
    }

    // POST api/creneaux — créer un créneau (Admin seulement)
    [Authorize(Roles = "Admin")]
    [HttpPost]
    public async Task<IActionResult> CreateCreneau([FromBody] Creneau creneau)
    {
        _context.Creneaux.Add(creneau);
        await _context.SaveChangesAsync();
        return CreatedAtAction(nameof(GetDisponibleCreneaux), new { id = creneau.Id }, creneau);
    }

    // DELETE api/creneaux/{id} — supprimer un créneau (Admin seulement)
    [Authorize(Roles = "Admin")]
    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteCreneau(int id)
    {
        var creneau = await _context.Creneaux.FindAsync(id);
        if (creneau == null)
            return NotFound();

        _context.Creneaux.Remove(creneau);
        await _context.SaveChangesAsync();
        return NoContent();
    }
}
